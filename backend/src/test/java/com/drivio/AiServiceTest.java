package com.drivio;

import com.drivio.dto.AiRequirementExtractionResponse;
import com.drivio.enums.ServiceType;
import com.drivio.enums.VehicleType;
import com.drivio.service.AiService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class AiServiceTest {

    private AiService aiService;

    @BeforeEach
    void setUp() {
        aiService = new AiService("http://localhost:11434", "llama3.1:8b");
    }

    @Test
    void testRuleBasedNlpFallback_OwnCarSuv() {
        String prompt = "I need an experienced driver tomorrow morning for my own SUV. I need to travel around 30 kilometres.";
        AiRequirementExtractionResponse response = aiService.extractUsingRuleBasedNlp(prompt);

        assertNotNull(response);
        assertEquals(ServiceType.OWN_CAR_DRIVER, response.getServiceType());
        assertEquals(VehicleType.SUV, response.getVehicleType());
        assertEquals("EXPERIENCED", response.getExperiencePreference());
        assertEquals(Double.valueOf(30.0), response.getEstimatedDistanceKm());
        assertEquals("09:00", response.getPickupTime());
        assertTrue(response.isFallbackUsed());
    }

    @Test
    void testRuleBasedNlpFallback_OutstationSedan() {
        String prompt = "Need driver next week for outstation trip from Delhi to Agra in a Sedan.";
        AiRequirementExtractionResponse response = aiService.extractUsingRuleBasedNlp(prompt);

        assertNotNull(response);
        assertEquals(ServiceType.OUTSTATION, response.getServiceType());
        assertEquals(VehicleType.SEDAN, response.getVehicleType());
        assertEquals("Delhi", response.getPickupLocationHint());
        assertEquals("Agra", response.getDestinationHint());
    }
}

