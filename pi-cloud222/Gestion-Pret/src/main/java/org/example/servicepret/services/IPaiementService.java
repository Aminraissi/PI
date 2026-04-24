package org.example.servicepret.services;

import org.example.servicepret.entities.Paiement;

import java.util.List;

public interface IPaiementService {

    public List<Paiement> retrieveAllPaiements();
    public Paiement updatePaiement (Paiement Paiement);
    public Paiement addPaiement (Paiement paiement);
    public Paiement retrievePaiement (long idPaiement);
    public void removePaiement(long idPaiement);
    public String creerPaymentIntent(long montant)throws Exception;
    public String creerPaymentIntentVirement(long montant) throws Exception;
    public String creerPaymentIntentAgriculteur(Long pretId) throws Exception;
}
