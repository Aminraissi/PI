package org.example.servicepret.services;


import org.example.servicepret.entities.Echeance;
import org.example.servicepret.entities.Pret;

import java.util.List;

public interface IEcheanceService {
    public List<Echeance> retrieveAllEcheances();
    public Echeance updateEcheance (Echeance echeance);
    public Echeance addEcheance (Echeance echeance);
    public Echeance retrieveEcheance (long idEcheance);
    public void removeEcheance(long idEcheance);
    public void generateEcheancesForPret(Pret pret);
    public List<Echeance> findByPretId(Long pretId);
    public void payerUneEcheance(Long echeanceId);
    public Echeance getNextPayableEcheance(Long pretId);
    public double calculMontantAvecPenalite(Echeance e);
}
