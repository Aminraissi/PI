package org.example.gestioninventaire.repositories;

import org.example.gestioninventaire.entities.Appointment;
import org.example.gestioninventaire.enums.AppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
    List<Appointment> findByFarmerId(Long farmerId);
    List<Appointment> findByVeterinarianId(Long veterinarianId);
    List<Appointment> findByAnimal_Id(Long animalId);
    long countByVeterinarianId(Long veterinarianId);
    long countByVeterinarianIdAndAppointmentStatus(Long veterinarianId, AppointmentStatus status);
    long countByVeterinarianIdAndDateHeureBetween(Long veterinarianId, LocalDateTime start, LocalDateTime end);
    long countByVeterinarianIdAndDateHeureAfter(Long veterinarianId, LocalDateTime dateTime);

    long countByFarmerId(Long farmerId);
    long countByFarmerIdAndAppointmentStatus(Long farmerId, AppointmentStatus status);
    long countByFarmerIdAndDateHeureAfter(Long farmerId, LocalDateTime dateTime);
    List<Appointment> findByAppointmentStatusAndDateHeureBetweenAndSmsReminderSentAtIsNull(
            AppointmentStatus status,
            LocalDateTime start,
            LocalDateTime end
    );

    List<Appointment> findByVeterinarianIdAndDateHeureAfterOrderByDateHeureAsc(Long veterinarianId, LocalDateTime dateTime);
    List<Appointment> findByFarmerIdAndDateHeureAfterOrderByDateHeureAsc(Long farmerId, LocalDateTime dateTime);

    boolean existsByVeterinarianIdAndAnimal_IdAndAppointmentStatus(
            Long veterinarianId,
            Long animalId,
            AppointmentStatus appointmentStatus
    );
}