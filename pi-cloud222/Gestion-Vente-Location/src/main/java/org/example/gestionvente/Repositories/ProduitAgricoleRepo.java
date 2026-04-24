package org.example.gestionvente.Repositories;


import org.example.gestionvente.Entities.ProduitAgricole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProduitAgricoleRepo extends JpaRepository<ProduitAgricole, Long> {
    List<ProduitAgricole> findByIdUser(Long idUser);
    List<ProduitAgricole> findByNomContainingIgnoreCase(String nom);
}