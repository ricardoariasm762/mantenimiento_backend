import numpy as np
import pandas as pd
from sklearn.ensemble import RandomForestClassifier
from sklearn.model_selection import train_test_split
from skl2onnx import convert_sklearn
from skl2onnx.common.data_types import FloatTensorType
import onnxruntime as ort

np.random.seed(42)
n_samples = 4000

# 1. Generación de telemetría sintética basada en fallas vehiculares reales
mileage = np.random.uniform(10000, 250000, n_samples)
km_since_service = np.random.exponential(scale=7000, size=n_samples)
km_since_service = np.clip(km_since_service, 200, 35000)

fuel_ratio = np.random.normal(loc=1.05, scale=0.18, size=n_samples)
fuel_ratio = np.clip(fuel_ratio, 0.85, 2.2)

symptom_code = np.random.randint(0, 5, size=n_samples)
severity = np.random.randint(1, 4, size=n_samples)

# Asignación de target con base en correlaciones técnicas
y = np.zeros(n_samples, dtype=int)

for i in range(n_samples):
    # Si la severidad es mínima y el servicio es reciente -> Estado Normal (5)
    if km_since_service[i] < 5000 and severity[i] == 1 and fuel_ratio[i] < 1.15:
        y[i] = 5
    else:
        # Correlación fuerte con el subsistema reportado y desgaste
        code = symptom_code[i]
        if code == 0:  # Transmisión: vibración en aceleración o desgaste mayor a 80,000 km
            y[i] = 0 if (mileage[i] > 60000 or severity[i] >= 2) else 5
        elif code == 1:  # Frenos: sensible a km desde último servicio
            y[i] = 1 if (km_since_service[i] > 12000 or severity[i] >= 2) else 5
        elif code == 2:  # Suspensión: sensible a kilometraje alto
            y[i] = 2 if (mileage[i] > 70000) else 5
        elif code == 3:  # Motor: consumo de combustible disparado
            y[i] = 3 if (fuel_ratio[i] > 1.30 or km_since_service[i] > 15000) else 5
        elif code == 4:  # Eléctrico: independiente de km, dependiente de severidad
            y[i] = 4 if severity[i] >= 2 else 5

X = np.column_stack([
    mileage.astype(np.float32),
    km_since_service.astype(np.float32),
    fuel_ratio.astype(np.float32),
    symptom_code.astype(np.float32),
    severity.astype(np.float32)
])

# 2. Entrenamiento del clasificador multiclase
X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.15, random_state=42)

clf = RandomForestClassifier(
    n_estimators=75,
    max_depth=8,
    random_state=42,
    n_jobs=-1
)
clf.fit(X_train, y_train)

accuracy = clf.score(X_test, y_test)
print(f"Entrenamiento finalizado. Exactitud (Accuracy) en prueba: {accuracy:.4f}")

# 3. Exportación a formato estándar ONNX
initial_type = [('float_input', FloatTensorType([None, 5]))]
onnx_model = convert_sklearn(
    clf,
    initial_types=initial_type,
    target_opset=12,
    options={id(clf): {'zipmap': False}}  # zipmap=False produce arrays directos de probabilidades
)

output_filename = "model_vehicle_fault.onnx"
with open(output_filename, "wb") as f:
    f.write(onnx_model.SerializeToString())

print(f"Modelo guardado como: {output_filename}")

# 4. Verificación de Inferencia con ONNX Runtime
session = ort.InferenceSession(output_filename)
input_name = session.get_inputs()[0].name

# Vector de prueba: [120000 km, 14000 km sin servicio, ratio 1.15, síntoma Transmisión (0), severidad 2]
test_vector = np.array([[120000.0, 14000.0, 1.15, 0.0, 2.0]], dtype=np.float32)
outputs = session.run(None, {input_name: test_vector})

predicted_class = outputs[0][0]
probabilities = outputs[1][0]
classes = ["Transmisión", "Frenos", "Suspensión", "Motor", "Eléctrico", "Normal"]

print(f"Predicción de prueba -> Clase: {classes[predicted_class]} con certeza: {probabilities[predicted_class]*100:.2f}%")