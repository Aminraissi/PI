package org.example.gestioninventaire.repositories;

import org.example.gestioninventaire.entities.VeterinarianUnavailability;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface VeterinarianUnavailabilityRepository extends JpaRepository<VeterinarianUnavailability, Long> {

    List<VeterinarianUnavailability> findByVeterinarianId(Long veterinarianId);

    // Requête explicite avec IS NOT NULL pour éviter les faux positifs sur valeurs nulles
    @Query("""
        SELECT u FROM VeterinarianUnavailability u
        WHERE u.veterinarianId = :veterinarianId
          AND u.startDate IS NOT NULL
          AND u.endDate IS NOT NULL
          AND u.startDate <= :date
          AND u.endDate >= :date
    """)
    List<VeterinarianUnavailability> findByVeterinarianIdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
            @Param("veterinarianId") Long veterinarianId,
            @Param("date") LocalDate date,
            @Param("date2") LocalDate date2
    );

    // Requête explicite avec IS NOT NULL pour les récurrents
    @Query("""
        SELECT u FROM VeterinarianUnavailability u
        WHERE u.veterinarianId = :veterinarianId
          AND u.recurringWeekly = true
          AND u.dayOfWeek = :dayOfWeek
          AND u.startDate IS NOT NULL
          AND u.endDate IS NOT NULL
    """)
    List<VeterinarianUnavailability> findByVeterinarianIdAndRecurringWeeklyTrueAndDayOfWeek(
            @Param("veterinarianId") Long veterinarianId,
            @Param("dayOfWeek") DayOfWeek dayOfWeek
    );
}
