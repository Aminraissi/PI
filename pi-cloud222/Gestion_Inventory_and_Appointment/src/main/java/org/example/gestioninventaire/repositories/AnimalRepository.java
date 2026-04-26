package org.example.gestioninventaire.repositories;
import org.example.gestioninventaire.entities.Animal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AnimalRepository extends JpaRepository<Animal, Long> {

    List<Animal> findByOwnerId(Long ownerId);
    java.util.Optional<Animal> findByIdAndOwnerIdAndIsDeletedFalse(Long id, Long ownerId);

    boolean existsByReference(String reference);
    @Query("SELECT a.reference FROM Animal a WHERE a.reference LIKE CONCAT(:prefix, '-%')")
    List<String> findReferencesByPrefix(@Param("prefix") String prefix);
    // Agriculteur : uniquement les animaux non supprimés
    List<Animal> findByOwnerIdAndIsDeletedFalse(Long ownerId);

    // Tous les animaux non supprimés (admin)
    List<Animal> findByIsDeletedFalse();
    // Campagne vaccination : exclure les animaux soft-deleted
    @Query("SELECT a FROM Animal a WHERE a.espece = :espece " +
            "AND a.isDeleted = false " +
            "AND YEAR(CURRENT_DATE) - YEAR(a.dateNaissance) >= :ageMin " +
            "AND YEAR(CURRENT_DATE) - YEAR(a.dateNaissance) <= :ageMax")
    List<Animal> findByEspeceAndAgeBetween(
            @Param("espece") String espece,
            @Param("ageMin") int ageMin,
            @Param("ageMax") int ageMax
    );




    @Query("SELECT a FROM Animal a WHERE a.espece = :espece " +
            "AND a.ownerId = :ownerId " +
            "AND a.isDeleted = false " +
            "AND YEAR(CURRENT_DATE) - YEAR(a.dateNaissance) >= :ageMin " +
            "AND YEAR(CURRENT_DATE) - YEAR(a.dateNaissance) <= :ageMax")
    List<Animal> findByOwnerIdAndEspeceAndAgeBetween(
            @Param("ownerId") Long ownerId,
            @Param("espece") String espece,
            @Param("ageMin") int ageMin,
            @Param("ageMax") int ageMax
    );


}
