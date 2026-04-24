package tn.esprit.livraison.controller.dto;

import java.time.LocalDateTime;

public record WeatherSnapshotDto(
        double windSpeedKmh,
        double precipitationMm,
        String condition,
        double surchargePercent,
        LocalDateTime fetchedAt,
        boolean fallback
) {
}
