package com.drivio.service;

import com.drivio.dto.AiRequirementExtractionRequest;
import com.drivio.dto.AiRequirementExtractionResponse;
import com.drivio.enums.ServiceType;
import com.drivio.enums.VehicleType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class AiService {

    private static final Logger logger = LoggerFactory.getLogger(AiService.class);

    private final String ollamaBaseUrl;
    private final String ollamaModel;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    public AiService(
            @Value("${drivio.ollama.base-url:http://localhost:11434}") String ollamaBaseUrl,
            @Value("${drivio.ollama.model:llama3.1:8b}") String ollamaModel) {
        this.ollamaBaseUrl = ollamaBaseUrl;
        this.ollamaModel = ollamaModel;
        this.objectMapper = new ObjectMapper();
        this.restTemplate = new RestTemplate();
    }

    public AiRequirementExtractionResponse extractRequirements(AiRequirementExtractionRequest request) {
        String prompt = request.getPrompt();

        // Attempt Ollama Llama 3.1 8B first
        try {
            return extractUsingOllama(prompt);
        } catch (Exception e) {
            logger.warn("Ollama AI service unavailable or returned invalid response ({}), falling back to deterministic NLP parser.", e.getMessage());
            return extractUsingRuleBasedNlp(prompt);
        }
    }

    private AiRequirementExtractionResponse extractUsingOllama(String userPrompt) throws Exception {
        String systemInstruction = """
                You are DRIVIO AI, an intelligent driver requirement extraction engine.
                Analyze the user's booking request and extract the parameters into STRICT JSON ONLY.
                No markdown fences, no conversational text, ONLY a valid JSON object.
                JSON Schema:
                {
                  "serviceType": "OWN_CAR_DRIVER" | "ONE_WAY" | "ROUND_TRIP" | "HOURLY" | "OUTSTATION",
                  "vehicleType": "HATCHBACK" | "SEDAN" | "SUV" | "LUXURY",
                  "experiencePreference": "EXPERIENCED" | "STANDARD" | "ANY",
                  "pickupDate": "YYYY-MM-DD",
                  "pickupTime": "HH:mm" or "MORNING" | "AFTERNOON" | "EVENING" | "NIGHT",
                  "estimatedDistanceKm": number or null,
                  "pickupLocationHint": string or null,
                  "destinationHint": string or null,
                  "additionalNotes": string or null
                }
                Today's date is """ + LocalDate.now().toString() + ".";

        Map<String, Object> payload = new HashMap<>();
        payload.put("model", ollamaModel);
        payload.put("prompt", systemInstruction + "\nUser request: \"" + userPrompt + "\"\nJSON:");
        payload.put("stream", false);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(ollamaBaseUrl + "/api/generate", entity, String.class);
        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new RuntimeException("Ollama returned status " + response.getStatusCode());
        }

        JsonNode rootNode = objectMapper.readTree(response.getBody());
        String responseText = rootNode.path("response").asText();

        // Extract JSON block if wrapped in markdown
        String jsonText = cleanJsonText(responseText);
        JsonNode extractedNode = objectMapper.readTree(jsonText);

        AiRequirementExtractionResponse result = new AiRequirementExtractionResponse();
        result.setRawExtraction(jsonText);
        result.setFallbackUsed(false);

        // Parse and validate serviceType
        String serviceTypeStr = extractedNode.path("serviceType").asText(null);
        if (serviceTypeStr != null) {
            try {
                result.setServiceType(ServiceType.valueOf(serviceTypeStr.toUpperCase()));
            } catch (IllegalArgumentException e) {
                result.setServiceType(ServiceType.OWN_CAR_DRIVER);
            }
        }

        // Parse and validate vehicleType
        String vehicleTypeStr = extractedNode.path("vehicleType").asText(null);
        if (vehicleTypeStr != null) {
            try {
                result.setVehicleType(VehicleType.valueOf(vehicleTypeStr.toUpperCase()));
            } catch (IllegalArgumentException e) {
                result.setVehicleType(VehicleType.SEDAN);
            }
        }

        result.setExperiencePreference(extractedNode.path("experiencePreference").asText("STANDARD"));
        result.setPickupDate(extractedNode.path("pickupDate").asText(null));
        result.setPickupTime(extractedNode.path("pickupTime").asText(null));

        if (extractedNode.has("estimatedDistanceKm") && !extractedNode.path("estimatedDistanceKm").isNull()) {
            result.setEstimatedDistanceKm(extractedNode.path("estimatedDistanceKm").asDouble());
        }

        result.setPickupLocationHint(extractedNode.path("pickupLocationHint").asText(null));
        result.setDestinationHint(extractedNode.path("destinationHint").asText(null));
        result.setAdditionalNotes(extractedNode.path("additionalNotes").asText(null));

        validateAndDetectMissing(result);
        return result;
    }

    public AiRequirementExtractionResponse extractUsingRuleBasedNlp(String text) {
        String lower = text.toLowerCase();
        AiRequirementExtractionResponse response = new AiRequirementExtractionResponse();
        response.setFallbackUsed(true);

        // Service Type detection
        if (lower.contains("own car") || lower.contains("my car") || lower.contains("my own")) {
            response.setServiceType(ServiceType.OWN_CAR_DRIVER);
        } else if (lower.contains("outstation") || lower.contains("another city") || lower.contains("intercity")) {
            response.setServiceType(ServiceType.OUTSTATION);
        } else if (lower.contains("round trip") || lower.contains("two way") || lower.contains("both ways")) {
            response.setServiceType(ServiceType.ROUND_TRIP);
        } else if (lower.contains("hourly") || lower.contains("hours") || lower.contains("per hour")) {
            response.setServiceType(ServiceType.HOURLY);
        } else if (lower.contains("one way") || lower.contains("drop")) {
            response.setServiceType(ServiceType.ONE_WAY);
        } else {
            response.setServiceType(ServiceType.OWN_CAR_DRIVER);
        }

        // Vehicle Type detection
        if (lower.contains("suv") || lower.contains("creta") || lower.contains("fortuner") || lower.contains("scorpio")) {
            response.setVehicleType(VehicleType.SUV);
        } else if (lower.contains("luxury") || lower.contains("bmw") || lower.contains("mercedes") || lower.contains("audi")) {
            response.setVehicleType(VehicleType.LUXURY);
        } else if (lower.contains("hatchback") || lower.contains("swift") || lower.contains("i10") || lower.contains("wagonr")) {
            response.setVehicleType(VehicleType.HATCHBACK);
        } else if (lower.contains("sedan") || lower.contains("city") || lower.contains("verna") || lower.contains("dzire")) {
            response.setVehicleType(VehicleType.SEDAN);
        } else {
            response.setVehicleType(VehicleType.SEDAN);
        }

        // Experience preference
        if (lower.contains("experienced") || lower.contains("expert") || lower.contains("senior") || lower.contains("veteran")) {
            response.setExperiencePreference("EXPERIENCED");
        } else {
            response.setExperiencePreference("STANDARD");
        }

        // Distance extraction (e.g. "30 km", "30 kilometers", "around 25kms")
        Pattern distancePattern = Pattern.compile("(\\d+(\\.\\d+)?)\\s*(km|kms|kilometres|kilometer|kilometers)", Pattern.CASE_INSENSITIVE);
        Matcher distMatcher = distancePattern.matcher(text);
        if (distMatcher.find()) {
            try {
                response.setEstimatedDistanceKm(Double.parseDouble(distMatcher.group(1)));
            } catch (NumberFormatException ignored) {}
        }

        // Date extraction
        LocalDate today = LocalDate.now();
        if (lower.contains("tomorrow")) {
            response.setPickupDate(today.plusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE));
        } else if (lower.contains("day after tomorrow")) {
            response.setPickupDate(today.plusDays(2).format(DateTimeFormatter.ISO_LOCAL_DATE));
        } else if (lower.contains("today") || lower.contains("tonight")) {
            response.setPickupDate(today.format(DateTimeFormatter.ISO_LOCAL_DATE));
        } else {
            response.setPickupDate(today.plusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE));
        }

        // Time of day
        if (lower.contains("morning")) {
            response.setPickupTime("09:00");
        } else if (lower.contains("afternoon")) {
            response.setPickupTime("14:00");
        } else if (lower.contains("evening")) {
            response.setPickupTime("18:00");
        } else if (lower.contains("night")) {
            response.setPickupTime("21:00");
        } else {
            // Check for explicit time like 10:30 AM or 11 AM
            Pattern timePattern = Pattern.compile("(\\d{1,2})(:(\\d{2}))?\\s*(am|pm)", Pattern.CASE_INSENSITIVE);
            Matcher timeMatcher = timePattern.matcher(text);
            if (timeMatcher.find()) {
                int hour = Integer.parseInt(timeMatcher.group(1));
                int min = timeMatcher.group(3) != null ? Integer.parseInt(timeMatcher.group(3)) : 0;
                String ampm = timeMatcher.group(4).toLowerCase();
                if (ampm.equals("pm") && hour < 12) hour += 12;
                if (ampm.equals("am") && hour == 12) hour = 0;
                response.setPickupTime(String.format("%02d:%02d", hour, min));
            } else {
                response.setPickupTime("10:00");
            }
        }

        // Locations hints (e.g. from Delhi to Agra, or from Airport)
        Pattern routePattern = Pattern.compile("from\\s+([A-Za-z0-9\\s]+?)\\s+to\\s+([A-Za-z0-9\\s]+?)(?:\\s+(?:in|by|for|with|a|an|the|at|tomorrow|today|next|on)|$)", Pattern.CASE_INSENSITIVE);
        Matcher routeMatcher = routePattern.matcher(text);
        if (routeMatcher.find()) {
            response.setPickupLocationHint(cleanLocation(routeMatcher.group(1)));
            response.setDestinationHint(cleanLocation(routeMatcher.group(2)));
        }

        response.setAdditionalNotes(text);
        validateAndDetectMissing(response);
        return response;
    }

    private void validateAndDetectMissing(AiRequirementExtractionResponse response) {
        List<String> missing = new ArrayList<>();
        if (response.getServiceType() == null) missing.add("Service Type");
        if (response.getVehicleType() == null) missing.add("Vehicle Type");
        if (response.getPickupDate() == null) missing.add("Pickup Date");
        if (response.getPickupTime() == null) missing.add("Pickup Time");
        if (response.getPickupLocationHint() == null || response.getPickupLocationHint().isBlank()) {
            missing.add("Pickup Location");
        }
        if (response.getDestinationHint() == null || response.getDestinationHint().isBlank()) {
            missing.add("Destination Location");
        }
        response.setMissingFields(missing);
    }

    private String cleanLocation(String loc) {
        if (loc == null) return null;
        return loc.replaceAll("(?i)\\b(in|by|for|with|a|an|the|at)\\b.*", "").trim();
    }

    private String cleanJsonText(String raw) {
        int startIndex = raw.indexOf('{');
        int endIndex = raw.lastIndexOf('}');
        if (startIndex != -1 && endIndex != -1 && endIndex > startIndex) {
            return raw.substring(startIndex, endIndex + 1);
        }
        return raw;
    }
}
