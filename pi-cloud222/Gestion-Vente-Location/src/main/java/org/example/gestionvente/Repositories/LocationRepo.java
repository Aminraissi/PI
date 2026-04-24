package org.example.gestionvente.Repositories;

import org.example.gestionvente.Entities.Location;
import org.example.gestionvente.Entities.TypeLocation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LocationRepo extends JpaRepository<Location, Long> {

    // 🔍 récupérer par user
    List<Location> findByIdUser(Long idUser);

    // 🔍 filtrer par type (terrain / materiel)
    List<Location> findByType(TypeLocation type);

    // 🔍 seulement disponibles
    List<Location> findByDisponibiliteTrue();
}