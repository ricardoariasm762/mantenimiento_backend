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
        // Initialize without ChatClient (pure heuristic fallback mode)
        symptomNlpService = new SymptomNlpService(Optional.empty(), new ObjectMapper());
    }

    @Test
    @DisplayName("Should classify Brake fault when squeals or brake pads are mentioned")
    void testExtractBrakesSymptom() {
        SymptomExtractionResult result = symptomNlpService.extractEntitiesFromSymptom("Siento que el pedal está esponjoso y chirría al frenar");

        assertNotNull(result);
        assertEquals(1, result.getSymptomSystemCode());
        assertEquals("Frenos", result.getDetectedComponent());
        assertTrue(result.getSymptomSeverity() >= 1);
    }

    @Test
    @DisplayName("Should classify Transmission fault on vibration or clutch slip issues")
    void testExtractTransmissionSymptom() {
        SymptomExtractionResult result = symptomNlpService.extractEntitiesFromSymptom("Vibración intensa en la palanca de cambios y el embrague patina");

        assertNotNull(result);
        assertEquals(0, result.getSymptomSystemCode());
        assertEquals("Transmisión", result.getDetectedComponent());
        assertTrue(result.getSymptomSeverity() >= 2);
    }

    @Test
    @DisplayName("Should classify Suspension fault on clunking noises over bumps")
    void testExtractSuspensionSymptom() {
        SymptomExtractionResult result = symptomNlpService.extractEntitiesFromSymptom("Golpeteo seco al pasar por baches y el carro rebota mucho");

        assertNotNull(result);
        assertEquals(2, result.getSymptomSystemCode());
        assertEquals("Suspensión", result.getDetectedComponent());
    }

    @Test
    @DisplayName("Should classify Engine fault when smoke or high temperature is present")
    void testExtractEngineSymptom() {
        SymptomExtractionResult result = symptomNlpService.extractEntitiesFromSymptom("Sale humo negro del escape y el motor cascabelea con alta temperatura");

        assertNotNull(result);
        assertEquals(3, result.getSymptomSystemCode());
        assertEquals("Motor", result.getDetectedComponent());
        assertEquals(3, result.getSymptomSeverity()); // Critical due to smoke / high temp
    }

    @Test
    @DisplayName("Should classify Electrical fault when battery or lights fail")
    void testExtractElectricalSymptom() {
        SymptomExtractionResult result = symptomNlpService.extractEntitiesFromSymptom("Testigo check engine encendido y las luces parpadean por la batería");

        assertNotNull(result);
        assertEquals(4, result.getSymptomSystemCode());
        assertEquals("Eléctrico", result.getDetectedComponent());
    }

    @Test
    @DisplayName("Should handle null or empty strings returning default state")
    void testExtractEmptySymptom() {
        SymptomExtractionResult resultNull = symptomNlpService.extractEntitiesFromSymptom(null);
        assertNotNull(resultNull);
        assertEquals(5, resultNull.getSymptomSystemCode());

        SymptomExtractionResult resultEmpty = symptomNlpService.extractEntitiesFromSymptom("   ");
        assertNotNull(resultEmpty);
        assertEquals(5, resultEmpty.getSymptomSystemCode());
    }
}
