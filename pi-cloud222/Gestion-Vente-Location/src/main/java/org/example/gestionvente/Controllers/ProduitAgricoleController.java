package org.example.gestionvente.Controllers;

import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.example.gestionvente.Services.IProduitAgricoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.example.gestionvente.Entities.ProduitAgricole;
import org.example.gestionvente.Services.ProduitAgricoleServiceImpl;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import jakarta.validation.Valid;

@RestController
@RequestMapping({"/api/produits", "/produitAgricoles"})
//@AllArgsConstructor
public class ProduitAgricoleController {

    @Autowired
    private IProduitAgricoleService service;

    @PostMapping
    public ProduitAgricole createProduit(@Valid @RequestBody ProduitAgricole produit) {
        return service.save(produit);
    }

    @PutMapping({"/update/{id}", "/{id}"})
    public ProduitAgricole update(@PathVariable("id") Long id, @RequestBody ProduitAgricole produit) {
        return service.update(id, produit);
    }

    @GetMapping
    public List<ProduitAgricole> getAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public ProduitAgricole getById(@PathVariable("id") Long id) {
        return service.findById(id);
    }

    @GetMapping("/user/{idUser}")
    public List<ProduitAgricole> getByUser(@PathVariable("idUser") Long idUser) {
        return service.findByUser(idUser);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable("id") Long id) {
        service.delete(id);
    }

}
