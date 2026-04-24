package org.example.gestioninventaire.mappers;

import lombok.RequiredArgsConstructor;
import org.example.gestioninventaire.dtos.response.AppointmentResponse;
import org.example.gestioninventaire.dtos.response.TimeSlotResponse;
import org.example.gestioninventaire.dtos.response.UserSummaryResponse;
import org.example.gestioninventaire.dtos.response.VeterinarianAvailabilityResponse;
import org.example.gestioninventaire.entities.Appointment;
import org.example.gestioninventaire.entities.TimeSlot;
import org.example.gestioninventaire.entities.VeterinarianAvailability;
import org.example.gestioninventaire.feigns.UserClient;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class AppointmentMapper {

    private final UserClient userClient;
    private final UserMapper userMapper;
    private final AnimalMapper animalMapper;

    public TimeSlotResponse toTimeSlotResponse(TimeSlot slot) {
        if (slot == null) return null;

        return TimeSlotResponse.builder()
                .id(slot.getId())
                .date(slot.getDate())
                .startTime(slot.getStartTime())
                .endTime(slot.getEndTime())
                .isBooked(slot.getIsBooked())
                .status(slot.getStatus())
                .build();
    }

    public AppointmentResponse toAppointmentResponse(Appointment appointment) {
        if (appointment == null) return null;

        UserSummaryResponse farmer = null;
        if (appointment.getFarmerId() != null) {
            try {
                farmer = userMapper.toSummary(userClient.getUserById(appointment.getFarmerId()));
            } catch (Exception e) {
                farmer = null;
            }
        }

        UserSummaryResponse veterinarian = null;
        if (appointment.getVeterinarianId() != null) {
            try {
                veterinarian = userMapper.toSummary(userClient.getUserById(appointment.getVeterinarianId()));
            } catch (Exception e) {
                veterinarian = null;
            }
        }

        return AppointmentResponse.builder()
                .id(appointment.getId())
                .dateHeure(appointment.getDateHeure())
                .motif(appointment.getMotif())
                .reason(appointment.getReason())
                .refusalReason(appointment.getRefusalReason())
                .createdAt(appointment.getCreatedAt())
                .appointmentStatus(appointment.getAppointmentStatus())
                .farmer(farmer)
                .veterinarian(veterinarian)
                .animal(animalMapper.toSummary(appointment.getAnimal()))
                .timeSlot(toTimeSlotResponse(appointment.getTimeSlot()))
                .build();
    }

    public VeterinarianAvailabilityResponse toAvailabilityResponse(
            VeterinarianAvailability availability,
            List<TimeSlot> slots
    ) {
        UserSummaryResponse veterinarian = null;
        if (availability.getVeterinarianId() != null) {
            try {
                veterinarian = userMapper.toSummary(userClient.getUserById(availability.getVeterinarianId()));
            } catch (Exception e) {
                veterinarian = null;
            }
        }

        return VeterinarianAvailabilityResponse.builder()
                .id(availability.getId())
                .date(availability.getDate())
                .bookedSlots(availability.getBookedSlots())
                .veterinarian(veterinarian)
                .timeSlots(slots.stream().map(this::toTimeSlotResponse).toList())
                .build();
    }
}
