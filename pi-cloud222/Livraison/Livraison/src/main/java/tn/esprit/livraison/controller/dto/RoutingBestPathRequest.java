package tn.esprit.livraison.controller.dto;

import java.util.List;

public record RoutingBestPathRequest(List<RoutingPoint> points) {
    public RoutingBestPathRequest {
        points = points == null ? List.of() : List.copyOf(points);
    }

    public record RoutingPoint(
            Double lat,
            Double lng,
            String label,
            String kind
    ) {
    }
}
