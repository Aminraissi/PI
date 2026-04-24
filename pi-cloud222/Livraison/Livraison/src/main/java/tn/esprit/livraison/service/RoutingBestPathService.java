package tn.esprit.livraison.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import tn.esprit.livraison.controller.dto.RoutingBestPathRequest;
import tn.esprit.livraison.controller.dto.RoutingBestPathResponse;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Service
public class RoutingBestPathService {

    private static final String OSRM_BASE_URL = "https://router.project-osrm.org";
    private static final Duration ROUTING_TIMEOUT = Duration.ofSeconds(6);

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public RoutingBestPathService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(ROUTING_TIMEOUT)
                .build();
    }

    public RoutingBestPathResponse computeBestPath(RoutingBestPathRequest request) {
        if (request == null || request.points() == null || request.points().size() < 2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least 2 points are required.");
        }

        List<RoutingBestPathRequest.RoutingPoint> points = request.points();

        for (RoutingBestPathRequest.RoutingPoint point : points) {
            validatePoint(point);
        }

        return requestRoadRoute(points);
    }

    private RoutingBestPathResponse requestRoadRoute(List<RoutingBestPathRequest.RoutingPoint> points) {
        String coords = points.stream()
                .map(point -> point.lng() + "," + point.lat())
                .reduce((left, right) -> left + ";" + right)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "No points provided."));

        String encodedCoords = URLEncoder.encode(coords, StandardCharsets.UTF_8).replace("%3B", ";").replace("%2C", ",");
        String url = OSRM_BASE_URL + "/route/v1/driving/" + encodedCoords + "?overview=full&geometries=geojson&steps=true";

        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(ROUTING_TIMEOUT)
                .GET()
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Routing service unavailable.");
            }

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode route = root.path("routes").isArray() && root.path("routes").size() > 0
                    ? root.path("routes").get(0)
                    : null;
            if (route == null || route.isMissingNode()) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "No route returned by routing service.");
            }

            List<RoutingBestPathResponse.RoutingPoint> polyline = parsePolyline(route.path("geometry").path("coordinates"));
            if (polyline.size() < 2) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Invalid route geometry from routing service.");
            }

            List<RoutingBestPathResponse.RoutingInstruction> instructions = parseInstructions(route.path("legs"));
            double distanceKm = round2(route.path("distance").asDouble(0.0) / 1000.0);
            int durationMinutes = Math.max(1, (int) Math.round(route.path("duration").asDouble(0.0) / 60.0));

            return new RoutingBestPathResponse(
                    polyline,
                    distanceKm,
                    durationMinutes,
                    durationMinutes,
                    instructions
            );
        } catch (IOException | InterruptedException ex) {
            if (ex instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Routing service unavailable.");
        }
    }

    private List<RoutingBestPathResponse.RoutingPoint> parsePolyline(JsonNode coordinatesNode) {
        List<RoutingBestPathResponse.RoutingPoint> polyline = new ArrayList<>();
        if (!coordinatesNode.isArray()) {
            return polyline;
        }

        for (JsonNode coordinate : coordinatesNode) {
            if (!coordinate.isArray() || coordinate.size() < 2) {
                continue;
            }
            double lng = coordinate.get(0).asDouble(Double.NaN);
            double lat = coordinate.get(1).asDouble(Double.NaN);
            if (Double.isFinite(lat) && Double.isFinite(lng)) {
                polyline.add(new RoutingBestPathResponse.RoutingPoint(lat, lng));
            }
        }
        return polyline;
    }

    private List<RoutingBestPathResponse.RoutingInstruction> parseInstructions(JsonNode legsNode) {
        List<RoutingBestPathResponse.RoutingInstruction> instructions = new ArrayList<>();
        if (!legsNode.isArray()) {
            return instructions;
        }

        for (JsonNode leg : legsNode) {
            JsonNode steps = leg.path("steps");
            if (!steps.isArray()) {
                continue;
            }
            for (JsonNode step : steps) {
                String name = step.path("name").asText("").trim();
                String text = name.isEmpty() ? "Continue" : "Continue on " + name;
                double stepDistanceKm = round2(step.path("distance").asDouble(0.0) / 1000.0);
                int stepDurationMinutes = Math.max(1, (int) Math.round(step.path("duration").asDouble(0.0) / 60.0));
                instructions.add(new RoutingBestPathResponse.RoutingInstruction(text, stepDistanceKm, stepDurationMinutes));
            }
        }

        return instructions;
    }

    private void validatePoint(RoutingBestPathRequest.RoutingPoint point) {
        if (point == null || point.lat() == null || point.lng() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Each point must contain lat and lng.");
        }

        double lat = point.lat();
        double lng = point.lng();
        if (!Double.isFinite(lat) || !Double.isFinite(lng) || lat < -90 || lat > 90 || lng < -180 || lng > 180) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid coordinates.");
        }
    }

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}

