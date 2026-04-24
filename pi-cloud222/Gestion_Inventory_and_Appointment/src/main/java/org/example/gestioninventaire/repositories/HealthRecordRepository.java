package org.example.gestioninventaire.repositories;

import org.example.gestioninventaire.entities.HealthRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HealthRecordRepository extends JpaRepository<HealthRecord, Long> {

    List<HealthRecord> findByAnimalId(Long animalId);

    // Charge l'animal en même temps pour éviter LazyInitializationException
    @Query("SELECT h FROM HealthRecord h JOIN FETCH h.animal WHERE h.id = :id")
    Optional<HealthRecord> findByIdWithAnimal(@Param("id") Long id);

    @Query("SELECT h FROM HealthRecord h JOIN FETCH h.animal")
    List<HealthRecord> findAllWithAnimal();

    @Query("SELECT h FROM HealthRecord h JOIN FETCH h.animal WHERE h.animal.id = :animalId")
    List<HealthRecord> findByAnimalIdWithAnimal(@Param("animalId") Long animalId);
}
