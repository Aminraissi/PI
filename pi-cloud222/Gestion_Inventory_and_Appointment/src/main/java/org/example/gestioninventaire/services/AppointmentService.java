package org.example.gestioninventaire.services;

import org.example.gestioninventaire.dtos.response.AppointmentStatsResponse;
import java.time.LocalDate;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.example.gestioninventaire.dtos.request.CreateAppointmentRequest;
import org.example.gestioninventaire.dtos.response.AppointmentResponse;
import org.example.gestioninventaire.entities.*;
import org.example.gestioninventaire.enums.AppointmentStatus;
import org.example.gestioninventaire.enums.SlotStatus;
import org.example.gestioninventaire.exceptions.BadRequestException;
import org.example.gestioninventaire.exceptions.ResourceNotFoundException;
import org.example.gestioninventaire.mappers.AppointmentMapper;
import org.example.gestioninventaire.repositories.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;


@Service
@RequiredArgsConstructor
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final TimeSlotRepository timeSlotRepository;
    private final VeterinarianAvailabilityRepository availabilityRepository;
    private final AnimalRepository animalRepository;
    private final AppointmentMapper appointmentMapper;

    @Transactional
    public AppointmentResponse createAppointment(CreateAppointmentRequest request, Long currentFarmerId) {

        TimeSlot slot = timeSlotRepository.findByIdForUpdate(request.getTimeSlotId())
                .orElseThrow(() -> new ResourceNotFoundException("Créneau non trouvé"));

        if (slot.getStatus() != SlotStatus.AVAILABLE || Boolean.TRUE.equals(slot.getIsBooked())) {
            throw new BadRequestException("Ce créneau n'est plus disponible");
        }

        // VeterinarianAvailability only has veterinarianId (no User object)
        if (!slot.getAvailability().getVeterinarianId().equals(request.getVeterinarianId())) {
            throw new BadRequestException("Le créneau ne correspond pas au vétérinaire sélectionné");
        }

        Animal animal = animalRepository.findById(request.getAnimalId())
                .orElseThrow(() -> new ResourceNotFoundException("Animal non trouvé"));

        Appointment appointment = Appointment.builder()
                .farmerId(currentFarmerId)
                .veterinarianId(request.getVeterinarianId())
                .animal(animal)
                .timeSlot(slot)
                .motif(request.getMotif())
                .reason(request.getReason())
                .createdAt(LocalDateTime.now())
                .dateHeure(LocalDateTime.of(slot.getDate(), slot.getStartTime()))
                .appointmentStatus(AppointmentStatus.EN_ATTENTE)
                .build();

        slot.setIsBooked(true);
        slot.setStatus(SlotStatus.BLOCKED);

        try {
            appointment = appointmentRepository.save(appointment);
            timeSlotRepository.save(slot);
        } catch (DataIntegrityViolationException ex) {
            throw new BadRequestException("Ce créneau a déjà été réservé par un autre utilisateur");
        }

        VeterinarianAvailability availability = slot.getAvailability();
        availability.setBookedSlots(
                availability.getBookedSlots() == null ? 1 : availability.getBookedSlots() + 1
        );
        availabilityRepository.save(availability);

        return appointmentMapper.toAppointmentResponse(appointment);
    }

    @Transactional
    public AppointmentResponse acceptAppointment(Long appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Rendez-vous non trouvé"));

        appointment.setAppointmentStatus(AppointmentStatus.ACCEPTEE);
        appointment.setSmsReminderSentAt(null);

        TimeSlot slot = appointment.getTimeSlot();
        slot.setStatus(SlotStatus.BOOKED);
        slot.setIsBooked(true);
        timeSlotRepository.save(slot);

        appointmentRepository.save(appointment);
        return appointmentMapper.toAppointmentResponse(appointment);
    }

    @Transactional
    public AppointmentResponse refuseAppointment(Long appointmentId, String reason) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Rendez-vous non trouvé"));

        appointment.setAppointmentStatus(AppointmentStatus.REFUSEE);
        appointment.setRefusalReason(reason);

        TimeSlot slot = appointment.getTimeSlot();
        slot.setStatus(SlotStatus.AVAILABLE);
        slot.setIsBooked(false);
        timeSlotRepository.save(slot);

        VeterinarianAvailability availability = slot.getAvailability();
        int current = availability.getBookedSlots() == null ? 0 : availability.getBookedSlots();
        availability.setBookedSlots(Math.max(0, current - 1));
        availabilityRepository.save(availability);

        appointmentRepository.save(appointment);
        return appointmentMapper.toAppointmentResponse(appointment);
    }

    @Transactional
    public AppointmentResponse cancelAppointment(Long appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Rendez-vous non trouvé"));

        appointment.setAppointmentStatus(AppointmentStatus.ANNULEE);

        TimeSlot slot = appointment.getTimeSlot();
        slot.setStatus(SlotStatus.AVAILABLE);
        slot.setIsBooked(false);
        timeSlotRepository.save(slot);

        VeterinarianAvailability availability = slot.getAvailability();
        int current = availability.getBookedSlots() == null ? 0 : availability.getBookedSlots();
        availability.setBookedSlots(Math.max(0, current - 1));
        availabilityRepository.save(availability);

        appointmentRepository.save(appointment);
        return appointmentMapper.toAppointmentResponse(appointment);
    }

    @Transactional(readOnly = true)
    public AppointmentStatsResponse getVetStats(Long vetId) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().plusDays(1).atStartOfDay();

        return AppointmentStatsResponse.builder()
                .totalAppointments(appointmentRepository.countByVeterinarianId(vetId))
                .pendingAppointments(appointmentRepository.countByVeterinarianIdAndAppointmentStatus(vetId, AppointmentStatus.EN_ATTENTE))
                .acceptedAppointments(appointmentRepository.countByVeterinarianIdAndAppointmentStatus(vetId, AppointmentStatus.ACCEPTEE))
                .refusedAppointments(appointmentRepository.countByVeterinarianIdAndAppointmentStatus(vetId, AppointmentStatus.REFUSEE))
                .cancelledAppointments(appointmentRepository.countByVeterinarianIdAndAppointmentStatus(vetId, AppointmentStatus.ANNULEE))
                .todayAppointments(appointmentRepository.countByVeterinarianIdAndDateHeureBetween(vetId, startOfDay, endOfDay))
                .upcomingAppointments(appointmentRepository.countByVeterinarianIdAndDateHeureAfter(vetId, now))
                .distinctAnimals(
                        appointmentRepository.findByVeterinarianId(vetId).stream()
                                .map(a -> a.getAnimal() != null ? a.getAnimal().getId() : null)
                                .filter(java.util.Objects::nonNull)
                                .collect(Collectors.toSet())
                                .size()
                )
                .build();
    }

    @Transactional(readOnly = true)
    public AppointmentStatsResponse getFarmerStats(Long farmerId) {
        LocalDateTime now = LocalDateTime.now();

        return AppointmentStatsResponse.builder()
                .totalAppointments(appointmentRepository.countByFarmerId(farmerId))
                .pendingAppointments(appointmentRepository.countByFarmerIdAndAppointmentStatus(farmerId, AppointmentStatus.EN_ATTENTE))
                .acceptedAppointments(appointmentRepository.countByFarmerIdAndAppointmentStatus(farmerId, AppointmentStatus.ACCEPTEE))
                .refusedAppointments(appointmentRepository.countByFarmerIdAndAppointmentStatus(farmerId, AppointmentStatus.REFUSEE))
                .cancelledAppointments(appointmentRepository.countByFarmerIdAndAppointmentStatus(farmerId, AppointmentStatus.ANNULEE))
                .todayAppointments(0) // optionnel pour agriculteur
                .upcomingAppointments(appointmentRepository.countByFarmerIdAndDateHeureAfter(farmerId, now))
                .distinctAnimals(
                        appointmentRepository.findByFarmerId(farmerId).stream()
                                .map(a -> a.getAnimal() != null ? a.getAnimal().getId() : null)
                                .filter(java.util.Objects::nonNull)
                                .collect(Collectors.toSet())
                                .size()
                )
                .build();
    }
}
