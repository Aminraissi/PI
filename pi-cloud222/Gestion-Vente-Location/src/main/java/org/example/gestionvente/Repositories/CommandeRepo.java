package org.example.gestionvente.Repositories;

import org.example.gestionvente.Entities.Commande;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface CommandeRepo extends JpaRepository<Commande, Long> {
    List<Commande> findByStatutAndDateCommandeBefore(String statut, LocalDateTime dateTime);

    List<Commande> findByUserIdAndStatutOrderByDateCommandeDesc(Long userId, String statut);

    List<Commande> findByUserId(Long userId);

    List<Commande> findByUserIdAndStatut(Long userId, String statut);

}