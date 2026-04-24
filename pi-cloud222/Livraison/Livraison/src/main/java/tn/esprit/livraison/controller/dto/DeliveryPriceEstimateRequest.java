package tn.esprit.livraison.controller.dto;

public record DeliveryPriceEstimateRequest(
        Double pickupLat,
        Double pickupLng,
        Double dropoffLat,
        Double dropoffLng,
        Double weightKg,
        Boolean autoGrouping,
        Double distanceKm,
        Double durationHours
) {
}
