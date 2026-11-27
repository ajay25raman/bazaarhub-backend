package com.bazaarhub.backend.delivery.infrastructure.adapter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Service integrating with Google Maps Platform (Directions and Geocoding APIs)
 * for calculating route distance, estimated transit time, and coordinate lookups.
 */
@Service
public class GoogleMapsService {

    private static final Logger log = LoggerFactory.getLogger(GoogleMapsService.class);
    private static final double EARTH_RADIUS_KM = 6371.0;

    @Value("${google.maps.api-key:placeholder_google_maps_key}")
    private String apiKey;

    private final HttpClient httpClient;

    public GoogleMapsService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    public record RouteDetails(BigDecimal distanceKm, int durationMinutes, String summary) {}

    /**
     * Estimates route distance and duration between two GPS coordinates or addresses.
     */
    public RouteDetails calculateRoute(BigDecimal originLat, BigDecimal originLng,
                                      BigDecimal destLat, BigDecimal destLng,
                                      String originAddress, String destAddress) {
        if (originLat != null && originLng != null && destLat != null && destLng != null) {
            // If live Google Maps API key configured, attempt real call
            if (apiKey != null && !apiKey.isBlank() && !"placeholder_google_maps_key".equals(apiKey)) {
                RouteDetails apiResult = queryDirectionsApi(
                        originLat + "," + originLng,
                        destLat + "," + destLng);
                if (apiResult != null) {
                    return apiResult;
                }
            }

            // Haversine fallback calculation
            double dLat = Math.toRadians(destLat.doubleValue() - originLat.doubleValue());
            double dLng = Math.toRadians(destLng.doubleValue() - originLng.doubleValue());
            double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                       Math.cos(Math.toRadians(originLat.doubleValue())) *
                       Math.cos(Math.toRadians(destLat.doubleValue())) *
                       Math.sin(dLng / 2) * Math.sin(dLng / 2);
            double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
            double straightDistanceKm = EARTH_RADIUS_KM * c;

            // Apply urban road winding factor of ~1.3
            double roadDistanceKm = straightDistanceKm * 1.3;
            if (roadDistanceKm < 0.5) {
                roadDistanceKm = 0.5;
            }

            // Assume average city speed 25 km/h + 5 min buffer
            int estimatedDurationMin = (int) Math.round((roadDistanceKm / 25.0) * 60) + 5;

            BigDecimal formattedDistance = BigDecimal.valueOf(roadDistanceKm).setScale(2, RoundingMode.HALF_UP);
            return new RouteDetails(formattedDistance, estimatedDurationMin, "Urban Route (Haversine modeled)");
        }

        // Default estimate when coordinates not fully available
        return new RouteDetails(BigDecimal.valueOf(5.00), 20, "Estimated standard urban transit");
    }

    private RouteDetails queryDirectionsApi(String origin, String destination) {
        try {
            String url = String.format("https://maps.googleapis.com/maps/api/directions/json?origin=%s&destination=%s&key=%s",
                    URLEncoder.encode(origin, StandardCharsets.UTF_8),
                    URLEncoder.encode(destination, StandardCharsets.UTF_8),
                    apiKey);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(6))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                String body = response.body();
                // Simple parser for distance (meters) and duration (seconds)
                int distIdx = body.indexOf("\"distance\"");
                int durIdx = body.indexOf("\"duration\"");
                if (distIdx != -1 && durIdx != -1) {
                    int valIdx = body.indexOf("\"value\":", distIdx);
                    int durValIdx = body.indexOf("\"value\":", durIdx);
                    if (valIdx != -1 && durValIdx != -1) {
                        int distMeters = Integer.parseInt(body.substring(valIdx + 8, body.indexOf(",", valIdx)).trim());
                        int durSeconds = Integer.parseInt(body.substring(durValIdx + 8, body.indexOf(",", durValIdx)).trim());
                        BigDecimal km = BigDecimal.valueOf(distMeters / 1000.0).setScale(2, RoundingMode.HALF_UP);
                        int min = (int) Math.ceil(durSeconds / 60.0);
                        return new RouteDetails(km, min, "Google Maps Route");
                    }
                }
            }
        } catch (Exception ex) {
            log.warn("[GOOGLE MAPS WARN] Failed to query Directions API: {}", ex.getMessage());
        }
        return null;
    }
}
