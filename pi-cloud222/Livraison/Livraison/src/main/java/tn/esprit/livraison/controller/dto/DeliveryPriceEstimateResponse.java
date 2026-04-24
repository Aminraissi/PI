package tn.esprit.livraison.controller.dto;

public record DeliveryPriceEstimateResponse(
        double estimatedPrice,
        double distanceKm,
        double durationHours,
        double weatherSurchargePercent,
        String weatherCondition,
        WeatherSnapshotDto weather
) {
}
