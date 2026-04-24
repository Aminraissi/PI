package org.exemple.paymentservice.repositories;

import org.exemple.paymentservice.entities.Paiement;
import org.exemple.paymentservice.enums.StatutPaiement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


@Repository
public interface PaiementRepository extends JpaRepository<Paiement, Long> {
    Optional<Paiement> findByReference(String reference);
    Optional<Paiement> findByFactureIdFacture(Long factureId);
    List<Paiement> findByFactureIdFactureOrderByDatePaiementDesc(Long factureId);
    List<Paiement> findByUserIdAndStatutOrderByDatePaiementDesc(Long userId, StatutPaiement statut);
}

