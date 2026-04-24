package org.example.gestionvente.Services;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.example.gestionvente.Entities.ProduitAgricole;
import org.example.gestionvente.Repositories.ProduitAgricoleRepo;

import java.util.List;

@Service
//@RequiredArgsConstructor
public class ProduitAgricoleServiceImpl implements IProduitAgricoleService {

    private ProduitAgricoleRepo repository;
    public ProduitAgricoleServiceImpl(ProduitAgricoleRepo repository) {
        this.repository = repository;
    }

    public ProduitAgricole save(ProduitAgricole produit) {
        return repository.save(produit);
    }

    public ProduitAgricole update(Long id, ProduitAgricole produit) {
        ProduitAgricole existing = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Produit not found"));

        existing.setNom(produit.getNom());
        existing.setDescription(produit.getDescription());
        existing.setPrix(produit.getPrix());
        existing.setQuantiteDisponible(produit.getQuantiteDisponible());
        existing.setPhotoProduit(produit.getPhotoProduit());
        existing.setIdUser(produit.getIdUser());
        existing.setCategory(produit.getCategory());

        return repository.save(existing);
    }

    public List<ProduitAgricole> findAll() {
        return repository.findAll();
    }

    public ProduitAgricole findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Produit not found"));
    }

    public List<ProduitAgricole> findByUser(Long idUser) {
        return repository.findByIdUser(idUser);
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }
}