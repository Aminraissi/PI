package org.example.gestioninventaire.repositories;

import org.example.gestioninventaire.entities.Commande;
import org.example.gestioninventaire.enums.StatutCommande;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CommandeRepository extends JpaRepository<Commande, Long> {

    List<Commande> findByAgriculteurIdOrderByDateCommandeDesc(Long agriculteurId);

    Optional<Commande> findByStripePaymentIntentId(String paymentIntentId);

    List<Commande> findByStatut(StatutCommande statut);

    /**
     * Retourne toutes les commandes qui contiennent au moins un item
     * appartenant au vétérinaire donné (vetId).
     */
    @Query("""
        SELECT DISTINCT c FROM Commande c
        JOIN c.items i
        WHERE i.vetId = :vetId
        ORDER BY c.dateCommande DESC
    """)
    List<Commande> findCommandesByVetId(@Param("vetId") Long vetId);
}