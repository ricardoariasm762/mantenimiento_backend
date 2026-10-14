package com.edu.vehiculos.service;

import com.edu.vehiculos.domain.dto.SymptomExtractionResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Optional;

@Service
@Slf4j
public class SymptomNlpService {

    private final Optional<ChatClient> chatClient;
    private final ObjectMapper objectMapper;

    @Autowired
    public SymptomNlpService(Optional<ChatClient.Builder> chatClientBuilder, ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.chatClient = chatClientBuilder.map(builder -> {
            try {
                return builder.build();
            } catch (Exception e) {
                log.warn("No se pudo inicializar ChatClient para Spring AI. Se usará el clasificador heurístico: {}", e.getMessage());
                return null;
            }
        });
    }

    public SymptomExtractionResult extractEntitiesFromSymptom(String symptomText) {
        if (symptomText == null || symptomText.trim().isEmpty()) {
            return SymptomExtractionResult.builder()
                    .symptomSystemCode(5)
                    .symptomSeverity(1)
                    .detectedComponent("Normal")
                    .build();
        }

        if (chatClient.isPresent() && chatClient.get() != null) {
            try {
                String systemPrompt = """
                        Eres un experto mecánico automotriz y analista de síntomas vehiculares.
                        Analiza la descripción del síntoma reportado por el conductor y extrae:
                        1. symptomSystemCode (entero de 0 a 4):
                           - 0: Transmisión (caja, embrague/clutch, vibración en aceleración, marchas)
                           - 1: Frenos (pastillas, discos, chirrido al frenar, pedal esponjoso, ABS)
                           - 2: Suspensión (amortiguadores, baches, golpes secos, balanceo, desalineación)
                           - 3: Motor (humo, consumo excesivo, aceite, cascabeleo, temperatura, pérdida de potencia)
                           - 4: Eléctrico (batería, alternador, luces, testigos, sensores, arranque)
                        2. symptomSeverity (entero de 1 a 3):
                           - 1: Leve (ruido intermitente o síntoma inicial)
                           - 2: Moderado (falla constante que afecta el manejo)
                           - 3: Crítico (riesgo inminente, inmovilización o peligro)
                        3. detectedComponent (texto con el nombre en español del sistema detectado: Transmisión, Frenos, Suspensión, Motor, o Eléctrico).
                        
                        Responde ÚNICAMENTE en formato JSON con la siguiente estructura exacta:
                        {"symptomSystemCode": 0, "symptomSeverity": 2, "detectedComponent": "Transmisión"}
                        """;

                String response = chatClient.get().prompt()
                        .system(systemPrompt)
                        .user("Síntoma reportado: " + symptomText)
                        .call()
                        .content();

                if (response != null && !response.isBlank()) {
                    String cleanJson = extractJsonSubstring(response);
                    JsonNode node = objectMapper.readTree(cleanJson);
                    int code = node.path("symptomSystemCode").asInt(0);
                    int severity = node.path("symptomSeverity").asInt(2);
                    String component = node.path("detectedComponent").asText(mapCodeToComponent(code));

                    // Asegurar límites válidos
                    if (code < 0 || code > 4) code = 0;
                    if (severity < 1 || severity > 3) severity = 2;

                    log.info("NLP LLM clasificación exitosa para síntoma '{}' -> Código: {}, Severidad: {}, Componente: {}",
                            symptomText, code, severity, component);

                    return SymptomExtractionResult.builder()
                            .symptomSystemCode(code)
                            .symptomSeverity(severity)
                            .detectedComponent(component)
                            .build();
                }
            } catch (Exception e) {
                log.warn("Falla al invocar servicio LLM o parsear respuesta ({}), activando fallback heurístico...", e.getMessage());
            }
        }

        return fallbackHeuristicClassification(symptomText);
    }

    private SymptomExtractionResult fallbackHeuristicClassification(String text) {
        String normalized = removeAccents(text.toLowerCase(Locale.ROOT));

        int code;
        String component;

        if (containsAny(normalized, "chirr", "fren", "pastill", "disco", "balat", "abs", "pedal")) {
            code = 1;
            component = "Frenos";
        } else if (containsAny(normalized, "vibr", "aceler", "caja", "embragu", "clutch", "marcha", "cambio", "patin", "traccion")) {
            code = 0;
            component = "Transmisión";
        } else if (containsAny(normalized, "bache", "golp", "suspens", "amortigua", "rebot", "direccion", "desaline", "buje", "estabil")) {
            code = 2;
            component = "Suspensión";
        } else if (containsAny(normalized, "bater", "luz", "luces", "electr", "alternad", "arranqu", "fusibl", "tabler", "sensor", "testigo", "check engine")) {
            code = 4;
            component = "Eléctrico";
        } else if (containsAny(normalized, "motor", "humo", "aceit", "gasolin", "consum", "calient", "temperatur", "revoluc", "potenc", "ahog", "cascabel")) {
            code = 3;
            component = "Motor";
        } else {
            // Predeterminado si no coincide
            code = 0;
            component = "Transmisión";
        }

        // Severidad heurística
        int severity = 1;
        if (containsAny(normalized, "humo", "fuego", "no frena", "bloque", "fuerte", "sever", "urgent", "peligr", "apago", "parar", "inmovil")) {
            severity = 3;
        } else if (containsAny(normalized, "constant", "moderad", "vibracion", "chirrido", "aument", "persistent", "mucho", "bastante")) {
            severity = 2;
        }

        log.info("Clasificación heurística de respaldo aplicada para '{}' -> Código: {}, Severidad: {}, Componente: {}",
                text, code, severity, component);

        return SymptomExtractionResult.builder()
                .symptomSystemCode(code)
                .symptomSeverity(severity)
                .detectedComponent(component)
                .build();
    }

    private boolean containsAny(String text, String... keywords) {
        for (String kw : keywords) {
            if (text.contains(kw)) {
                return true;
            }
        }
        return false;
    }

    private String removeAccents(String text) {
        String normalized = Normalizer.normalize(text, Normalizer.Form.NFD);
        return normalized.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
    }

    private String extractJsonSubstring(String text) {
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start != -1 && end != -1 && end > start) {
            return text.substring(start, end + 1);
        }
        return text;
    }

    private String mapCodeToComponent(int code) {
        return switch (code) {
            case 0 -> "Transmisión";
            case 1 -> "Frenos";
            case 2 -> "Suspensión";
            case 3 -> "Motor";
            case 4 -> "Eléctrico";
            default -> "Sistema Mecánico General";
        };
    }
}
