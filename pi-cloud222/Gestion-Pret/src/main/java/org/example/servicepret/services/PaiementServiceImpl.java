package org.example.servicepret.services;

import com.stripe.Stripe;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import jakarta.annotation.PostConstruct;
import org.example.servicepret.entities.Echeance;
import org.example.servicepret.entities.Paiement;
import org.example.servicepret.repositories.PaiementRepo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PaiementServiceImpl implements IPaiementService {

    private final PaiementRepo paiementRepo;
    private  final IEcheanceService echeanceService;

    public PaiementServiceImpl(PaiementRepo paiementRepo,
                               IEcheanceService echeanceService) {
        this.paiementRepo = paiementRepo;
        this.echeanceService = echeanceService;
    }

    @Value("${stripe.secret.key}")
    private String stripeSecretKey;

    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeSecretKey;
    }

    public List<Paiement> retrieveAllPaiements() {
        return paiementRepo.findAll();
    }

    public Paiement updatePaiement(Paiement paiement) {
        return paiementRepo.save(paiement);
    }

    public Paiement addPaiement(Paiement paiement) {
        return paiementRepo.save(paiement);
    }

    public Paiement retrievePaiement(long idPaiement) {
        return paiementRepo.findById(idPaiement).orElse(null);
    }

    public void removePaiement(long idPaiement) {
        paiementRepo.deleteById(idPaiement);
    }

    public String creerPaymentIntentVirement(long montant) throws Exception {
        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount(montant * 100)
                .setCurrency("eur")
                .addPaymentMethodType("sepa_debit")
                .setDescription("Prêt agricole")
                .build();

        PaymentIntent intent = PaymentIntent.create(params);
        return intent.getClientSecret();
    }

    public String creerPaymentIntent(long montant) throws Exception {

        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount(montant * 100)
                .setCurrency("eur")
                .setDescription("Paiement de prêt agricole")
                .setAutomaticPaymentMethods(
                        PaymentIntentCreateParams.AutomaticPaymentMethods
                                .builder()
                                .setEnabled(true)
                                .build()
                )
                .build();

        PaymentIntent intent = PaymentIntent.create(params);
        return intent.getClientSecret();
    }
    @Override
    public String creerPaymentIntentAgriculteur(Long echeanceId) throws Exception {

        Echeance e = echeanceService.retrieveEcheance(echeanceId);

        if (e == null) {
            throw new RuntimeException("Échéance introuvable");
        }

        double montant = echeanceService.calculMontantAvecPenalite(e);

        System.out.println("Paiement échéance ID = " + e.getId());
        System.out.println("Montant calculé = " + montant);

        PaymentIntentCreateParams params =
                PaymentIntentCreateParams.builder()
                        .setAmount(Math.round(montant * 100))
                        .setCurrency("eur")
                        .setDescription("Paiement UNE échéance")
                        .putMetadata("echeanceId", String.valueOf(e.getId()))
                        .setAutomaticPaymentMethods(
                                PaymentIntentCreateParams.AutomaticPaymentMethods
                                        .builder()
                                        .setEnabled(true)
                                        .build()
                        )
                        .build();

        PaymentIntent intent = PaymentIntent.create(params);

        return intent.getClientSecret();
    }

}