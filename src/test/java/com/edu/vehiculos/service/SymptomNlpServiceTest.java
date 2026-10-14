package com.edu.vehiculos.service;

import com.edu.vehiculos.domain.dto.SymptomExtractionResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class SymptomNlpServiceTest {

    private SymptomNlpService symptomNlpService;

    @BeforeEach
    void setUp() {
        // Inicializar sin ChatClient (modo puramente heurístico de respaldo)
        symptomNlpService = new SymptomNlpService(Optional.empty(), new ObjectMapper());
    }

    @Test
    @DisplayName("Debe clasificar falla en Frenos cuando se mencionan chirridos o pastillas")
    void testExtractBrakesSymptom() {
        SymptomExtractionResult result = symptomNlpService.extractEntitiesFromSymptom("Siento que el pedal está esponjoso y chirría al frenar");

        assertNotNull(result);
        assertEquals(1, result.getSymptomSystemCode());
        assertEquals("Frenos", result.getDetectedComponent());
        assertTrue(result.getSymptomSeverity() >= 1);
    }

    @Test
    @DisplayName("Debe clasificar falla en Transmisión ante vibración o problemas de caja/clutch")
    void testExtractTransmissionSymptom() {
        SymptomExtractionResult result = symptomNlpService.extractEntitiesFromSymptom("Vibración intensa en la palanca de cambios y el embrague patina");

        assertNotNull(result);
        assertEquals(0, result.getSymptomSystemCode());
        assertEquals("Transmisión", result.getDetectedComponent());
        assertTrue(result.getSymptomSeverity() >= 2);
    }

    @Test
    @DisplayName("Debe clasificar falla en Suspensión con golpes en baches y amortiguadores")
    void testExtractSuspensionSymptom() {
        SymptomExtractionResult result = symptomNlpService.extractEntitiesFromSymptom("Golpeteo seco al pasar por baches y el carro rebota mucho");

        assertNotNull(result);
        assertEquals(2, result.getSymptomSystemCode());
        assertEquals("Suspensión", result.getDetectedComponent());
    }

    @Test
    @DisplayName("Debe clasificar falla de Motor con humo o consumo excesivo")
    void testExtractEngineSymptom() {
        SymptomExtractionResult result = symptomNlpService.extractEntitiesFromSymptom("Sale humo negro del escape y el motor cascabelea con alta temperatura");

        assertNotNull(result);
        assertEquals(3, result.getSymptomSystemCode());
        assertEquals("Motor", result.getDetectedComponent());
        assertEquals(3, result.getSymptomSeverity()); // Crítico por humo / temperatura
    }

    @Test
    @DisplayName("Debe clasificar falla Eléctrica cuando falla la batería o luces")
    void testExtractElectricalSymptom() {
        SymptomExtractionResult result = symptomNlpService.extractEntitiesFromSymptom("Testigo check engine encendido y las luces parpadean por la batería");

        assertNotNull(result);
        assertEquals(4, result.getSymptomSystemCode());
        assertEquals("Eléctrico", result.getDetectedComponent());
    }

    @Test
    @DisplayName("Debe manejar cadenas nulas o vacías retornando estado por defecto")
    void testExtractEmptySymptom() {
        SymptomExtractionResult resultNull = symptomNlpService.extractEntitiesFromSymptom(null);
        assertNotNull(resultNull);
        assertEquals(5, resultNull.getSymptomSystemCode());

        SymptomExtractionResult resultEmpty = symptomNlpService.extractEntitiesFromSymptom("   ");
        assertNotNull(resultEmpty);
        assertEquals(5, resultEmpty.getSymptomSystemCode());
    }
}
