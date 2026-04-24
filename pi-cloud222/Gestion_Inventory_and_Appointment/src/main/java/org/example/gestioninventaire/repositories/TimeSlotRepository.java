package org.example.gestioninventaire.repositories;

import jakarta.persistence.LockModeType;
import org.example.gestioninventaire.entities.TimeSlot;
import org.example.gestioninventaire.enums.SlotStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
@Repository
public interface TimeSlotRepository extends JpaRepository<TimeSlot, Long> {
    List<TimeSlot> findByAvailabilityVeterinarianIdAndDateAndStatus(Long vetId, LocalDate date, SlotStatus status);

    List<TimeSlot> findByAvailabilityId(Long availabilityId);

    // Check if a slot with same date+time already exists under any availability of this vet
    @Query("SELECT t FROM TimeSlot t WHERE t.availability.veterinarianId = :vetId " +
            "AND t.date = :date AND t.startTime = :startTime AND t.endTime = :endTime")
    List<TimeSlot> findByVetIdAndDateAndTime(
            @Param("vetId") Long vetId,
            @Param("date") java.time.LocalDate date,
            @Param("startTime") java.time.LocalTime startTime,
            @Param("endTime") java.time.LocalTime endTime
    );

    // All slots for a vet on a given date
    @Query("SELECT t FROM TimeSlot t WHERE t.availability.veterinarianId = :vetId AND t.date = :date")
    List<TimeSlot> findByVetIdAndDate(
            @Param("vetId") Long vetId,
            @Param("date") java.time.LocalDate date
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from TimeSlot t where t.id = :id")

    Optional<TimeSlot> findByIdForUpdate(@Param("id") Long id);
}