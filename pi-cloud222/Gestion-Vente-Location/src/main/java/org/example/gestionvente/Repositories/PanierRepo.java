package org.example.gestionvente.Repositories;

import org.example.gestionvente.Entities.Panier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PanierRepo extends JpaRepository<Panier, Long> {

    Optional<Panier> findByIdUserAndStatut(Long idUser, String statut);
}