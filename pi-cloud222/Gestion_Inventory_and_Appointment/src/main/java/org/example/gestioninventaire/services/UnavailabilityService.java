package org.example.gestioninventaire.services;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.example.gestioninventaire.dtos.request.CreateUnavailabilityRequest;
import org.example.gestioninventaire.dtos.response.UnavailabilityResponse;
import org.example.gestioninventaire.entities.TimeSlot;
import org.example.gestioninventaire.entities.VeterinarianAvailability;
import org.example.gestioninventaire.entities.VeterinarianUnavailability;
import org.example.gestioninventaire.enums.SlotStatus;
import org.example.gestioninventaire.exceptions.BadRequestException;
import org.example.gestioninventaire.exceptions.ResourceNotFoundException;
import org.example.gestioninventaire.mappers.UnavailabilityMapper;
import org.example.gestioninventaire.repositories.TimeSlotRepository;
import org.example.gestioninventaire.repositories.VeterinarianAvailabilityRepository;
import org.example.gestioninventaire.repositories.VeterinarianUnavailabilityRepository;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UnavailabilityService {

    private final VeterinarianUnavailabilityRepository unavailabilityRepository;
    private final VeterinarianAvailabilityRepository availabilityRepository;
    private final TimeSlotRepository timeSlotRepository;
    private final UnavailabilityMapper unavailabilityMapper;

    @Transactional
    public void createUnavailability(Long veterinarianId, CreateUnavailabilityRequest request) {

        if (request.getStartDate().isAfter(request.getEndDate())) {
            throw new BadRequestException("La date de debut doit etre avant ou egale a la date de fin");
        }

        if (!request.getFullDay()) {
            if (request.getStartTime() == null || request.getEndTime() == null) {
                throw new BadRequestException("Les heures sont obligatoires pour un blocage partiel");
            }
            if (!request.getStartTime().isBefore(request.getEndTime())) {
                throw new BadRequestException("L'heure de debut doit etre avant l'heure de fin");
            }
        }

        if (request.getRecurringWeekly() && request.getDayOfWeek() == null) {
            throw new BadRequestException("Le jour de semaine est obligatoire pour un blocage recurrent");
        }

        ensureNoOverlappingDates(veterinarianId, request);

        VeterinarianUnavailability unavailability = VeterinarianUnavailability.builder()
                .veterinarianId(veterinarianId)
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .fullDay(request.getFullDay())
                .recurringWeekly(request.getRecurringWeekly())
                .dayOfWeek(request.getDayOfWeek())
                .reason(request.getReason())
                .build();

        unavailabilityRepository.save(unavailability);

        LocalDate current = request.getStartDate();
        while (!current.isAfter(request.getEndDate())) {

            boolean applies =
                    !request.getRecurringWeekly() ||
                            current.getDayOfWeek().equals(request.getDayOfWeek());

            if (applies) {
                applyBlockingToDate(veterinarianId, current, request);
            }

            current = current.plusDays(1);
        }
    }

    private void ensureNoOverlappingDates(Long veterinarianId, CreateUnavailabilityRequest request) {
        List<VeterinarianUnavailability> existingUnavailabilities =
                unavailabilityRepository.findByVeterinarianId(veterinarianId);

        for (VeterinarianUnavailability existing : existingUnavailabilities) {
            if (hasDateConflict(existing, request)) {
                throw new BadRequestException(
                        "Impossible de creer l'indisponibilite : une periode existe deja sur ces dates"
                );
            }
        }
    }

    private boolean hasDateConflict(VeterinarianUnavailability existing, CreateUnavailabilityRequest request) {
        if (existing.getStartDate() == null || existing.getEndDate() == null) {
            return false;
        }

        LocalDate overlapStart = maxDate(existing.getStartDate(), request.getStartDate());
        LocalDate overlapEnd = minDate(existing.getEndDate(), request.getEndDate());

        if (overlapStart.isAfter(overlapEnd)) {
            return false;
        }

        boolean existingRecurring = Boolean.TRUE.equals(existing.getRecurringWeekly());
        boolean requestRecurring = Boolean.TRUE.equals(request.getRecurringWeekly());

        if (!existingRecurring && !requestRecurring) {
            return true;
        }

        if (existingRecurring && requestRecurring) {
            return existing.getDayOfWeek() != null
                    && request.getDayOfWeek() != null
                    && existing.getDayOfWeek().equals(request.getDayOfWeek());
        }

        if (existingRecurring) {
            return existing.getDayOfWeek() != null
                    && rangeContainsDay(overlapStart, overlapEnd, existing.getDayOfWeek());
        }

        return request.getDayOfWeek() != null
                && rangeContainsDay(overlapStart, overlapEnd, request.getDayOfWeek());
    }

    private boolean rangeContainsDay(LocalDate start, LocalDate end, DayOfWeek dayOfWeek) {
        int currentValue = start.getDayOfWeek().getValue();
        int targetValue = dayOfWeek.getValue();
        int delta = (targetValue - currentValue + 7) % 7;
        LocalDate firstMatch = start.plusDays(delta);
        return !firstMatch.isAfter(end);
    }

    private LocalDate maxDate(LocalDate left, LocalDate right) {
        return left.isAfter(right) ? left : right;
    }

    private LocalDate minDate(LocalDate left, LocalDate right) {
        return left.isBefore(right) ? left : right;
    }

    private void applyBlockingToDate(Long veterinarianId, LocalDate date, CreateUnavailabilityRequest request) {
        List<VeterinarianAvailability> availabilities =
                availabilityRepository.findByVeterinarianIdAndDate(veterinarianId, date);

        for (VeterinarianAvailability availability : availabilities) {
            List<TimeSlot> slots = timeSlotRepository.findByAvailabilityId(availability.getId());

            for (TimeSlot slot : slots) {

                if (slot.getStatus() == SlotStatus.BOOKED) {
                    continue;
                }

                if (request.getFullDay()) {
                    slot.setStatus(SlotStatus.BLOCKED);
                    slot.setIsBooked(false);
                    timeSlotRepository.save(slot);
                } else {
                    boolean overlap =
                            !slot.getEndTime().isBefore(request.getStartTime()) &&
                                    !slot.getStartTime().isAfter(request.getEndTime());

                    if (overlap) {
                        slot.setStatus(SlotStatus.BLOCKED);
                        slot.setIsBooked(false);
                        timeSlotRepository.save(slot);
                    }
                }
            }
        }
    }

    @Transactional
    public void deleteUnavailability(Long veterinarianId, Long unavailabilityId) {
        VeterinarianUnavailability unavailability = unavailabilityRepository.findById(unavailabilityId)
                .orElseThrow(() -> new ResourceNotFoundException("Indisponibilite non trouvee"));

        if (!unavailability.getVeterinarianId().equals(veterinarianId)) {
            throw new BadRequestException("Vous ne pouvez pas supprimer cette indisponibilite");
        }

        LocalDate current = unavailability.getStartDate();
        while (!current.isAfter(unavailability.getEndDate())) {

            boolean applies =
                    !Boolean.TRUE.equals(unavailability.getRecurringWeekly()) ||
                            current.getDayOfWeek().equals(unavailability.getDayOfWeek());

            if (applies) {
                removeBlockingFromDate(veterinarianId, current, unavailability);
            }

            current = current.plusDays(1);
        }

        unavailabilityRepository.delete(unavailability);
    }

    private void removeBlockingFromDate(
            Long veterinarianId,
            LocalDate date,
            VeterinarianUnavailability unavailability
    ) {
        List<VeterinarianAvailability> availabilities =
                availabilityRepository.findByVeterinarianIdAndDate(veterinarianId, date);

        for (VeterinarianAvailability availability : availabilities) {
            List<TimeSlot> slots = timeSlotRepository.findByAvailabilityId(availability.getId());

            for (TimeSlot slot : slots) {

                if (slot.getStatus() == SlotStatus.BOOKED) {
                    continue;
                }

                if (Boolean.TRUE.equals(unavailability.getFullDay())) {
                    slot.setStatus(SlotStatus.AVAILABLE);
                    slot.setIsBooked(false);
                    timeSlotRepository.save(slot);
                } else {
                    boolean overlap =
                            slot.getStartTime().isBefore(unavailability.getEndTime()) &&
                                    slot.getEndTime().isAfter(unavailability.getStartTime());

                    if (overlap) {
                        slot.setStatus(SlotStatus.AVAILABLE);
                        slot.setIsBooked(false);
                        timeSlotRepository.save(slot);
                    }
                }
            }
        }
    }

    public boolean isBlocked(Long veterinarianId, LocalDate date, LocalTime startTime, LocalTime endTime) {

        List<VeterinarianUnavailability> directBlocks =
                unavailabilityRepository.findByVeterinarianIdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                        veterinarianId, date, date
                );

        List<VeterinarianUnavailability> recurringBlocks =
                unavailabilityRepository.findByVeterinarianIdAndRecurringWeeklyTrueAndDayOfWeek(
                        veterinarianId, date.getDayOfWeek()
                );

        List<VeterinarianUnavailability> allBlocks = new java.util.ArrayList<>();
        allBlocks.addAll(directBlocks);
        allBlocks.addAll(recurringBlocks);

        for (VeterinarianUnavailability block : allBlocks) {
            if (Boolean.TRUE.equals(block.getFullDay())) {
                return true;
            }
            if (block.getStartTime() != null && block.getEndTime() != null) {
                boolean overlap = startTime.isBefore(block.getEndTime())
                        && endTime.isAfter(block.getStartTime());
                if (overlap) {
                    return true;
                }
            }
        }

        return false;
    }

    public List<UnavailabilityResponse> getByVeterinarian(Long veterinarianId) {
        return unavailabilityRepository.findByVeterinarianId(veterinarianId)
                .stream()
                .map(unavailabilityMapper::toResponse)
                .toList();
    }
}
