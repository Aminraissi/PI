package org.example.gestioninventaire.services;

import lombok.RequiredArgsConstructor;
import org.example.gestioninventaire.dtos.request.CreateAvailabilityRequest;
import org.example.gestioninventaire.dtos.response.UserResponse;
import org.example.gestioninventaire.dtos.response.VeterinarianAvailabilityResponse;
import org.example.gestioninventaire.entities.TimeSlot;
import org.example.gestioninventaire.entities.VeterinarianAvailability;
import org.example.gestioninventaire.entities.VeterinarianUnavailability;
import org.example.gestioninventaire.enums.SlotStatus;
import org.example.gestioninventaire.exceptions.BadRequestException;
import org.example.gestioninventaire.exceptions.ResourceNotFoundException;
import org.example.gestioninventaire.feigns.UserClient;
import org.example.gestioninventaire.mappers.AppointmentMapper;
import org.example.gestioninventaire.repositories.TimeSlotRepository;
import org.example.gestioninventaire.repositories.VeterinarianAvailabilityRepository;
import org.example.gestioninventaire.repositories.VeterinarianUnavailabilityRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AvailabilityService {

    private final UserClient userClient;
    private final VeterinarianAvailabilityRepository availabilityRepository;
    private final TimeSlotRepository timeSlotRepository;
    private final AppointmentMapper appointmentMapper;
    private final UnavailabilityService unavailabilityService;
    private final VeterinarianUnavailabilityRepository unavailabilityRepository;

    @Transactional
    public VeterinarianAvailabilityResponse createAvailability(CreateAvailabilityRequest request) {

        // 1. Validate veterinarian
        UserResponse veterinarian;
        try {
            veterinarian = userClient.getUserById(request.getVeterinarianId());
        } catch (Exception e) {
            throw new ResourceNotFoundException("Vétérinaire non trouvé");
        }

        if (!"VETERINAIRE".equals(veterinarian.getRole())) {
            throw new BadRequestException("L'utilisateur sélectionné n'est pas un vétérinaire");
        }

        if (request.getDate().isBefore(LocalDate.now())) {
            throw new BadRequestException("La date de disponibilite ne peut pas etre inferieure a la date d'aujourd'hui");
        }
        // 2. Validate time range
        if (!request.getStartTime().isBefore(request.getEndTime())) {
            throw new BadRequestException("L'heure de début doit être avant l'heure de fin");
        }

        // 3. Check for full-day blocks (indisponibilités)
        List<VeterinarianUnavailability> fullDayBlocks =
                unavailabilityRepository.findByVeterinarianIdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                        request.getVeterinarianId(), request.getDate(), request.getDate()
                );

        boolean hasFullDayBlock = fullDayBlocks.stream().anyMatch(block ->
                Boolean.TRUE.equals(block.getFullDay()) &&
                        (!Boolean.TRUE.equals(block.getRecurringWeekly())
                                || request.getDate().getDayOfWeek().equals(block.getDayOfWeek()))
        );

        if (hasFullDayBlock) {
            throw new BadRequestException("Impossible de créer une disponibilité : cette journée est bloquée");
        }

        // 4. Reuse existing VeterinarianAvailability for this date, or create one
        List<VeterinarianAvailability> existingForDay =
                availabilityRepository.findByVeterinarianIdAndDate(
                        request.getVeterinarianId(), request.getDate()
                );

        VeterinarianAvailability availability;
        if (!existingForDay.isEmpty()) {
            // Reuse the first existing availability record for this day
            availability = existingForDay.get(0);
        } else {
            // Create a new one only if none exists for this day
            availability = VeterinarianAvailability.builder()
                    .veterinarianId(request.getVeterinarianId())
                    .date(request.getDate())
                    .bookedSlots(0)
                    .build();
            availability = availabilityRepository.save(availability);
        }

        // 5. Generate only NEW slots — skip any slot that already exists for this vet/date/time
        LocalTime current = request.getStartTime();
        int newSlotsCreated = 0;

        while (current.isBefore(request.getEndTime())) {
            LocalTime next = current.plusMinutes(request.getSlotDurationMinutes());
            if (next.isAfter(request.getEndTime())) {
                break;
            }

            // Check if this exact slot already exists (any status: AVAILABLE, BOOKED, BLOCKED)
            List<TimeSlot> existing = timeSlotRepository.findByVetIdAndDateAndTime(
                    request.getVeterinarianId(),
                    request.getDate(),
                    current,
                    next
            );

            if (existing.isEmpty()) {
                // Slot doesn't exist → create it
                boolean blocked = unavailabilityService.isBlocked(
                        request.getVeterinarianId(),
                        request.getDate(),
                        current,
                        next
                );

                TimeSlot slot = TimeSlot.builder()
                        .date(request.getDate())
                        .startTime(current)
                        .endTime(next)
                        .isBooked(false)
                        .status(blocked ? SlotStatus.BLOCKED : SlotStatus.AVAILABLE)
                        .availability(availability)
                        .build();

                timeSlotRepository.save(slot);
                newSlotsCreated++;
            }
            // If slot already exists (BOOKED or AVAILABLE) → leave it untouched

            current = next;
        }

        if (newSlotsCreated == 0) {
            throw new BadRequestException(
                    "Tous les créneaux de cette plage horaire existent déjà pour cette date. " +
                            "Aucun nouveau créneau créé."
            );
        }

        List<TimeSlot> allSlots = timeSlotRepository.findByAvailabilityId(availability.getId());
        return appointmentMapper.toAvailabilityResponse(availability, allSlots);
    }

    public List<VeterinarianAvailabilityResponse> getVetAvailabilities(Long veterinarianId) {
        List<VeterinarianAvailability> availabilities =
                availabilityRepository.findByVeterinarianId(veterinarianId);

        return availabilities.stream()
                .map(av -> appointmentMapper.toAvailabilityResponse(
                        av,
                        timeSlotRepository.findByAvailabilityId(av.getId())
                ))
                .toList();
    }

    @Transactional
    public void blockDay(Long veterinarianId, LocalDate date) {
        List<VeterinarianAvailability> availabilities =
                availabilityRepository.findByVeterinarianIdAndDate(veterinarianId, date);

        if (availabilities.isEmpty()) {
            throw new ResourceNotFoundException("Aucune disponibilité trouvée pour ce jour");
        }

        for (VeterinarianAvailability availability : availabilities) {
            List<TimeSlot> slots = timeSlotRepository.findByAvailabilityId(availability.getId());
            for (TimeSlot slot : slots) {
                if (slot.getStatus() == SlotStatus.BOOKED) continue; // preserve bookings
                slot.setStatus(SlotStatus.BLOCKED);
                slot.setIsBooked(false);
                timeSlotRepository.save(slot);
            }
        }
    }
}


