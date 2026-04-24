package org.example.gestioninventaire.mappers;

import org.example.gestioninventaire.dtos.response.UnavailabilityResponse;
import org.example.gestioninventaire.entities.VeterinarianUnavailability;
import org.springframework.stereotype.Component;

@Component
public class UnavailabilityMapper {

    public UnavailabilityResponse toResponse(VeterinarianUnavailability entity) {
        return UnavailabilityResponse.builder()
                .id(entity.getId())
                .veterinarianId(entity.getVeterinarianId())
                .startDate(entity.getStartDate())
                .endDate(entity.getEndDate())
                .startTime(entity.getStartTime())
                .endTime(entity.getEndTime())
                .fullDay(entity.getFullDay())
                .recurringWeekly(entity.getRecurringWeekly())
                .dayOfWeek(entity.getDayOfWeek())
                .reason(entity.getReason())
                .build();
    }
}
