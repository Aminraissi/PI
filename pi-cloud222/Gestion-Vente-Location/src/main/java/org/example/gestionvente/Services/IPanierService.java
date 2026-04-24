package org.example.gestionvente.Services;

import org.example.gestionvente.Entities.Panier;
import org.example.gestionvente.Dtos.PanierDetailDto;


public interface IPanierService {
    Panier getOrCreatePanier(Long idUser);
    PanierDetailDto getPanierDetails(Long idUser);
}