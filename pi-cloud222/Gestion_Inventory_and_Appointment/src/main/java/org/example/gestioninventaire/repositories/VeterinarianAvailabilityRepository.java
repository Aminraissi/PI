package org.example.gestioninventaire.repositories;

import org.example.gestioninventaire.entities.VeterinarianAvailability;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface VeterinarianAvailabilityRepository extends JpaRepository<VeterinarianAvailability, Long> {

    List<VeterinarianAvailability> findByVeterinarianId(Long veterinarianId);

    List<VeterinarianAvailability> findByVeterinarianIdAndDate(Long veterinarianId, LocalDate date);
}
