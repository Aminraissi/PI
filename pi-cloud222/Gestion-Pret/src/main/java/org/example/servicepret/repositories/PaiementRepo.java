package org.example.servicepret.repositories;

import org.example.servicepret.entities.Paiement;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaiementRepo extends JpaRepository<Paiement,Long> {
}
