package org.example.gestionvente.Repositories;

import org.example.gestionvente.Entities.Panier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PanierRepo extends JpaRepository<Panier, Long> {

    Panier findFirstByIdUserAndStatutOrderByDateCreationDescIdDesc(Long idUser, String statut);
}