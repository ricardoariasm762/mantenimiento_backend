package com.edu.vehiculos.engine;

import ai.onnxruntime.*;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class VehicleFaultInferenceEngine {

    @Value("${onnx.model.path:models/model_vehicle_fault.onnx}")
    private String modelPath;

    private final ResourceLoader resourceLoader;
    private OrtEnvironment env;
    private OrtSession session;

    public static final String[] CLASSES = {
            "Transmisión",
            "Frenos",
            "Suspensión",
            "Motor",
            "Eléctrico",
            "Normal"
    };

    @Getter
    public static class PredictionResult {
        private final int classIndex;
        private final String predictedFault;
        private final double confidencePercentage;

        public PredictionResult(int classIndex, String predictedFault, double confidencePercentage) {
            this.classIndex = classIndex;
            this.predictedFault = predictedFault;
            this.confidencePercentage = confidencePercentage;
        }
    }

    public VehicleFaultInferenceEngine(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @PostConstruct
    public void init() {
        try {
            log.info("Inicializando ONNX Runtime Environment...");
            this.env = OrtEnvironment.getEnvironment();
            OrtSession.SessionOptions opts = new OrtSession.SessionOptions();

            String fullPath = modelPath.startsWith("classpath:") ? modelPath : "classpath:" + modelPath;
            Resource resource = resourceLoader.getResource(fullPath);

            if (!resource.exists()) {
                throw new IllegalStateException("El archivo del modelo ONNX no se encontró en: " + fullPath);
            }

            try (InputStream is = resource.getInputStream()) {
                byte[] modelBytes = is.readAllBytes();
                this.session = env.createSession(modelBytes, opts);
            }

            log.info("Modelo ONNX cargado exitosamente desde {}. Entradas: {}", fullPath, session.getInputNames());
        } catch (Exception e) {
            log.error("Error al cargar el modelo ONNX: {}", e.getMessage(), e);
            throw new RuntimeException("No se pudo inicializar el motor de inferencia ONNX", e);
        }
    }

    public PredictionResult predict(double mileageKm, double kmSinceLastService, double fuelConsumptionRatio,
                                   int symptomSystemCode, int symptomSeverity) {
        try {
            float[][] inputData = new float[][]{
                    {
                            (float) mileageKm,
                            (float) kmSinceLastService,
                            (float) fuelConsumptionRatio,
                            (float) symptomSystemCode,
                            (float) symptomSeverity
                    }
            };

            String inputName = session.getInputNames().iterator().next();

            try (OnnxTensor inputTensor = OnnxTensor.createTensor(env, inputData);
                 OrtSession.Result results = session.run(Collections.singletonMap(inputName, inputTensor))) {

                // Obtener etiqueta predicha (primer tensor)
                OnnxValue labelValue = results.get(0);
                long[] labels = (long[]) labelValue.getValue();
                int predictedClass = (int) labels[0];

                // Obtener probabilidades (segundo tensor)
                double confidence = 85.0; // Valor seguro predeterminado
                if (results.size() > 1) {
                    OnnxValue probValue = results.get(1);
                    Object probObj = probValue.getValue();

                    if (probObj instanceof float[][] probMatrix) {
                        if (predictedClass >= 0 && predictedClass < probMatrix[0].length) {
                            confidence = probMatrix[0][predictedClass] * 100.0;
                        }
                    } else if (probObj instanceof List<?> list && !list.isEmpty()) {
                        Object firstElem = list.get(0);
                        if (firstElem instanceof Map<?, ?> map) {
                            Object prob = map.get((long) predictedClass);
                            if (prob instanceof Number num) {
                                confidence = num.doubleValue() * 100.0;
                            }
                        }
                    }
                }

                // Redondear a 2 decimales
                confidence = Math.round(confidence * 100.0) / 100.0;

                String predictedFaultName = (predictedClass >= 0 && predictedClass < CLASSES.length)
                        ? CLASSES[predictedClass]
                        : "Desconocido";

                log.info("Inferencia ONNX completada -> Clase predicha: {} ({}), Certeza: {}%",
                        predictedClass, predictedFaultName, confidence);

                return new PredictionResult(predictedClass, predictedFaultName, confidence);
            }
        } catch (Exception e) {
            log.error("Error durante la inferencia ONNX: {}", e.getMessage(), e);
            throw new RuntimeException("Fallo en la predicción del modelo ONNX", e);
        }
    }

    @PreDestroy
    public void cleanup() {
        try {
            log.info("Cerrando recursos de ONNX Runtime...");
            if (session != null) {
                session.close();
            }
            if (env != null) {
                env.close();
            }
        } catch (Exception e) {
            log.warn("Excepción al cerrar recursos de ONNX: {}", e.getMessage());
        }
    }
}
