package com.drivio.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Service
public class RoutingService {

    private static final Logger logger = LoggerFactory.getLogger(RoutingService.class);

    private final String osrmBaseUrl;
    private final String nominatimBaseUrl;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public RoutingService(
            @Value("${drivio.routing.osrm-url:https://router.project-osrm.org}") String osrmBaseUrl,
            @Value("${drivio.routing.nominatim-url:https://nominatim.openstreetmap.org}") String nominatimBaseUrl) {
        this.osrmBaseUrl = osrmBaseUrl;
        this.nominatimBaseUrl = nominatimBaseUrl;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(3));
        factory.setReadTimeout(Duration.ofSeconds(5));
        this.restTemplate = new RestTemplate(factory);
    }

    public RouteResult calculateRoute(double pickupLat, double pickupLng, double destLat, double destLng) {
        try {
            String url = String.format(
                    "%s/route/v1/driving/%f,%f;%f,%f?overview=full&geometries=geojson",
                    osrmBaseUrl, pickupLng, pickupLat, destLng, destLat
            );
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode routes = root.path("routes");
                if (routes.isArray() && !routes.isEmpty()) {
                    JsonNode first = routes.get(0);
                    double distanceKm = Math.round((first.path("distance").asDouble() / 1000.0) * 100.0) / 100.0;
                    int durationMinutes = Math.max(1, (int) Math.round(first.path("duration").asDouble() / 60.0));
                    List<double[]> coordinates = new ArrayList<>();
                    JsonNode coordsNode = first.path("geometry").path("coordinates");
                    if (coordsNode.isArray()) {
                        for (JsonNode pt : coordsNode) {
                            // GeoJSON is [lng, lat]; return [lat, lng] for Leaflet
                            coordinates.add(new double[]{pt.get(1).asDouble(), pt.get(0).asDouble()});
                        }
                    }
                    return new RouteResult(distanceKm, durationMinutes, "OSRM", coordinates);
                }
            }
        } catch (Exception ex) {
            logger.warn("OSRM routing unavailable ({}), falling back to Haversine estimate", ex.getMessage());
        }

        double haversineKm = haversine(pickupLat, pickupLng, destLat, destLng);
        double roadFactorKm = Math.round(haversineKm * 1.25 * 100.0) / 100.0;
        int estimatedMins = Math.max(5, (int) Math.round((roadFactorKm / 30.0) * 60.0));
        List<double[]> fallbackCoords = List.of(
                new double[]{pickupLat, pickupLng},
                new double[]{destLat, destLng}
        );
        return new RouteResult(roadFactorKm, estimatedMins, "HAVERSINE_FALLBACK", fallbackCoords);
    }

    private double haversine(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371;
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    public record RouteResult(double distanceKm, int durationMinutes, String provider, List<double[]> geometry) {}

    public record GeocodeSuggestion(String displayName, double latitude, double longitude, String provider) {}

    public List<GeocodeSuggestion> searchAddress(String query) {
        if (query == null || query.trim().length() < 2) {
            return List.of();
        }
        try {
            String url = UriComponentsBuilder.fromHttpUrl(nominatimBaseUrl + "/search")
                    .queryParam("q", query.trim())
                    .queryParam("format", "json")
                    .queryParam("limit", 5)
                    .queryParam("countrycodes", "in")
                    .toUriString();
            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "DRIVIO-Mobility/1.0 (contact@drivio.com)");
            ResponseEntity<String> resp = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), String.class);
            if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
                JsonNode arr = objectMapper.readTree(resp.getBody());
                List<GeocodeSuggestion> results = new ArrayList<>();
                if (arr.isArray()) {
                    for (JsonNode item : arr) {
                        results.add(new GeocodeSuggestion(
                                item.path("display_name").asText(),
                                item.path("lat").asDouble(),
                                item.path("lon").asDouble(),
                                "NOMINATIM"
                        ));
                    }
                }
                if (!results.isEmpty()) return results;
            }
        } catch (Exception ex) {
            logger.warn("Nominatim geocode unavailable ({}), returning Delhi NCR fallback suggestions", ex.getMessage());
        }

        // Offline/fallback Delhi NCR landmarks
        String qLower = query.toLowerCase();
        List<GeocodeSuggestion> local = List.of(
                new GeocodeSuggestion("Connaught Place, New Delhi, India", 28.6280, 77.2180, "LOCAL_FALLBACK"),
                new GeocodeSuggestion("Sector 62, Noida, Uttar Pradesh, India", 28.6250, 77.3750, "LOCAL_FALLBACK"),
                new GeocodeSuggestion("DLF Cyber City, Gurugram, Haryana, India", 28.4950, 77.0895, "LOCAL_FALLBACK"),
                new GeocodeSuggestion("Indira Gandhi International Airport, New Delhi", 28.5562, 77.1000, "LOCAL_FALLBACK"),
                new GeocodeSuggestion("India Gate, New Delhi, India", 28.6129, 77.2295, "LOCAL_FALLBACK"),
                new GeocodeSuggestion("Saket District Centre, New Delhi, India", 28.5245, 77.2066, "LOCAL_FALLBACK")
        );
        return local.stream()
                .filter(s -> s.displayName().toLowerCase().contains(qLower))
                .toList();
    }

    public GeocodeSuggestion reverseGeocode(double lat, double lng) {
        try {
            String url = UriComponentsBuilder.fromHttpUrl(nominatimBaseUrl + "/reverse")
                    .queryParam("lat", lat)
                    .queryParam("lon", lng)
                    .queryParam("format", "json")
                    .toUriString();
            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "DRIVIO-Mobility/1.0 (contact@drivio.com)");
            ResponseEntity<String> resp = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), String.class);
            if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
                JsonNode node = objectMapper.readTree(resp.getBody());
                String name = node.path("display_name").asText(null);
                if (name != null && !name.isBlank()) {
                    return new GeocodeSuggestion(name, lat, lng, "NOMINATIM");
                }
            }
        } catch (Exception ex) {
            logger.warn("Nominatim reverse geocode unavailable ({})", ex.getMessage());
        }
        return new GeocodeSuggestion(String.format("Pinned Location (%.4f, %.4f)", lat, lng), lat, lng, "LOCAL_FALLBACK");
    }
}
