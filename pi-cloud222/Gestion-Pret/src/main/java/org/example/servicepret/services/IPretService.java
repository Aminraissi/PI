package org.example.servicepret.services;


import org.example.servicepret.entities.Pret;

import java.util.List;

public interface IPretService {
    public List<Pret> retrieveAllPrets();
    public Pret updatePret (Pret Pret);
    public Pret addPret (Pret Pret);
    public Pret retrievePret (long idPret);
    public void removePret(long idPret);
    Pret findByDemandeId(Long demandeId);
    public List<Pret> getPretByAgriculteur(Long agriculteurId);
}
