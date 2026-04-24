package org.example.servicepret.services;

import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.example.servicepret.entities.*;
import org.example.servicepret.repositories.EcheanceRepo;
import org.example.servicepret.repositories.PaiementRepo;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@AllArgsConstructor
public class EcheanceServiceImpl implements IEcheanceService{
private EcheanceRepo echeanceRepo;
private PaiementRepo paiementRepo;
    public List<Echeance> retrieveAllEcheances()
    {
        return echeanceRepo.findAll();
    }
    public Echeance updateEcheance (Echeance echeance)
    {
        return echeanceRepo.save(echeance);
    }
    public Echeance addEcheance (Echeance echeance)
    {
        return echeanceRepo.save(echeance);
    }
    public Echeance retrieveEcheance (long idEcheance)
    {
        return echeanceRepo.findById(idEcheance).orElse(null);
    }
    public void removeEcheance(long idEcheance)
    {
        echeanceRepo.deleteById(idEcheance);

    }
    @Override
    public void generateEcheancesForPret(Pret pret) {

        int nb = pret.getNbEcheances();
        double montantParEcheance = pret.getMontantTotal() / nb;

        LocalDate dateDebut = pret.getDateDebut();

        for (int i = 0; i < nb; i++) {

            Echeance e = new Echeance();

            e.setMontant(montantParEcheance);
            e.setStatut(StatutEcheance.EN_ATTENTE);

            e.setDateDebut(dateDebut.plusMonths(i));
            e.setDateFin(dateDebut.plusMonths(i + 1));

            e.setPret(pret);

            echeanceRepo.save(e);
        }
    }
    @Transactional
    @Override
    public List<Echeance> findByPretId(Long pretId) {

        List<Echeance> list = echeanceRepo.findByPretId(pretId);

        updateStatuts(list);

        return list;
    }
    @Override
    public void payerUneEcheance(Long echeanceId) {

        Echeance e = echeanceRepo.findById(echeanceId)
                .orElseThrow(() -> new RuntimeException("Échéance introuvable"));

        double montantFinal = calculMontantAvecPenalite(e);

        Paiement p = new Paiement();
        p.setDatePaiement(LocalDate.now());
        p.setMontant(montantFinal);
        p.setModePaiement("STRIPE");
        p.setEcheance(e);

        paiementRepo.save(p);

        e.setStatut(StatutEcheance.PAYEE);
        echeanceRepo.save(e);
    }
    @Override
    public double calculMontantAvecPenalite(Echeance e) {

        double base = e.getMontant();

        if (e.getStatut() != StatutEcheance.EN_RETARD)
            return base;

        long joursRetard = java.time.temporal.ChronoUnit.DAYS
                .between(e.getDateFin(), LocalDate.now());

        if (joursRetard <= 0) return base;

        double taux = 0.02;



        Pret pret = e.getPret();

        if (pret != null) {

            DemandePret demande = pret.getDemande();

            if (demande != null && demande.getService() != null) {
                taux = demande.getService().getTauxPenalite();
            }
        }

        double mois = Math.ceil(joursRetard / 30.0);

        return base + (base * taux * mois);
    }
    @Override
    public Echeance getNextPayableEcheance(Long pretId) {

        List<Echeance> echeances = echeanceRepo.findByPretId(pretId);

        if (echeances.isEmpty()) return null;

        echeances.sort((a, b) -> a.getDateDebut().compareTo(b.getDateDebut()));

        Echeance e = echeances.stream()
                .filter(x -> x.getStatut() == StatutEcheance.EN_RETARD)
                .findFirst()
                .orElse(null);

        if (e == null) {
            e = echeances.stream()
                    .filter(x -> x.getStatut() == StatutEcheance.EN_ATTENTE)
                    .findFirst()
                    .orElse(null);
        }

        return e;
    }

    private void updateStatuts(List<Echeance> list) {

        for (Echeance e : list) {

            if (e.getStatut() == StatutEcheance.PAYEE) continue;

            if (e.getDateFin().isBefore(java.time.LocalDate.now())) {
                e.setStatut(StatutEcheance.EN_RETARD);
            }
        }
    }
    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void updateStatutEcheances() {

        List<Echeance> echeances =
                echeanceRepo.findByStatutNot(StatutEcheance.PAYEE);

        for (Echeance e : echeances) {

            if (e.getDateFin() != null
                    && e.getDateFin().isBefore(LocalDate.now())) {

                e.setStatut(StatutEcheance.EN_RETARD);
            }
        }

        echeanceRepo.saveAll(echeances);
    }

}
