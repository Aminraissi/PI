package org.example.gestionvente.Services;

import org.example.gestionvente.Entities.ProduitAgricole;

import java.util.List;

public interface IProduitAgricoleService {
    ProduitAgricole save(ProduitAgricole produit);
    List<ProduitAgricole> findAll();
    ProduitAgricole findById(Long id);
    List<ProduitAgricole> findByUser(Long idUser);
    void delete(Long id);
    ProduitAgricole update(Long id, ProduitAgricole produit);
}
