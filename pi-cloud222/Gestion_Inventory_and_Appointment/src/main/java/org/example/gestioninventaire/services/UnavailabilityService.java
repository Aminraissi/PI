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

        if (!request.getFullDay()) {
            if (request.getStartTime() == null || request.getEndTime() == null) {
                throw new BadRequestException("Les heures sont obligatoires pour un blocage partiel");
            }
            if (!request.getStartTime().isBefore(request.getEndTime())) {
                throw new BadRequestException("L'heure de début doit être avant l'heure de fin");
            }
        }

        if (request.getRecurringWeekly() && request.getDayOfWeek() == null) {
            throw new BadRequestException("Le jour de semaine est obligatoire pour un blocage récurrent");
        }

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

        // appliquer seulement sur les disponibilités déjà créées
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
                .orElseThrow(() -> new ResourceNotFoundException("Indisponibilité non trouvée"));

        if (!unavailability.getVeterinarianId().equals(veterinarianId)) {
            throw new BadRequestException("Vous ne pouvez pas supprimer cette indisponibilité");
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

        // Blocs directs sur cette date (startDate et endDate NOT NULL garantis par la query)
        List<VeterinarianUnavailability> directBlocks =
                unavailabilityRepository.findByVeterinarianIdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                        veterinarianId, date, date
                );

        // Blocs récurrents pour ce jour de semaine (NOT NULL garantis par la query)
        List<VeterinarianUnavailability> recurringBlocks =
                unavailabilityRepository.findByVeterinarianIdAndRecurringWeeklyTrueAndDayOfWeek(
                        veterinarianId, date.getDayOfWeek()
                );

        List<VeterinarianUnavailability> allBlocks = new java.util.ArrayList<>();
        allBlocks.addAll(directBlocks);
        allBlocks.addAll(recurringBlocks);

        for (VeterinarianUnavailability block : allBlocks) {
            // Bloc journée entière → bloqué
            if (Boolean.TRUE.equals(block.getFullDay())) {
                return true;
            }
            // Bloc horaire → vérifier le chevauchement
            if (block.getStartTime() != null && block.getEndTime() != null) {
                boolean overlap = startTime.isBefore(block.getEndTime())
                        && endTime.isAfter(block.getStartTime());
                if (overlap) return true;
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
