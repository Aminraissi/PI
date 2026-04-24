package tn.esprit.livraison.controller.dto;

import java.util.List;

public record RoutingBestPathResponse(
        List<RoutingPoint> polyline,
        double distanceKm,
        int durationMinutes,
        int etaMinutes,
        List<RoutingInstruction> instructions
) {
    public RoutingBestPathResponse {
        polyline = polyline == null ? List.of() : List.copyOf(polyline);
        instructions = instructions == null ? List.of() : List.copyOf(instructions);
    }

    public record RoutingPoint(double lat, double lng) {
    }

    public record RoutingInstruction(String text, double distanceKm, int durationMinutes) {
    }
}
