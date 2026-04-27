package org.exemple.paymentservice.services.impl;

import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.model.SetupIntent;
import com.stripe.model.checkout.Session;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.checkout.SessionCreateParams;
import lombok.RequiredArgsConstructor;
import org.exemple.paymentservice.dtos.CreateRentalPaymentPlanRequest;
import org.exemple.paymentservice.entities.PaiementLocation;
import org.exemple.paymentservice.repositories.PaiementLocationRepository;
import org.exemple.paymentservice.services.RentalPaymentService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
public class RentalPaymentServiceImpl implements RentalPaymentService {

    private final PaiementLocationRepository paiementLocationRepository;

    @Override
    public List<PaiementLocation> getAllPayments() {
        return paiementLocationRepository.findAll();
    }

    @Override
    public List<PaiementLocation> createPaymentPlan(CreateRentalPaymentPlanRequest request) {

        if (paiementLocationRepository.existsByPropositionId(request.getPropositionId())) {
            return paiementLocationRepository.findByPropositionIdOrderByMoisNumeroAsc(request.getPropositionId());
        }

        List<PaiementLocation> payments = new ArrayList<>();

        for (int i = 1; i <= request.getNbMois(); i++) {

            double monthlyAmount = request.getMontantMensuel();
            double commissionRate = 0.10; // 10%
            double commissionAmount = monthlyAmount * commissionRate;
            double farmerAmount = monthlyAmount - commissionAmount;

            PaiementLocation p = PaiementLocation.builder()
                    .propositionId(request.getPropositionId())
                    .locationId(request.getLocationId())
                    .locataireId(request.getLocataireId())
                    .agriculteurId(request.getAgriculteurId())
                    .moisNumero(i)
                    .montant(monthlyAmount)
                    .commissionRate(commissionRate)
                    .commissionAmount(commissionAmount)
                    .farmerAmount(farmerAmount)
                    .dateEcheance(request.getDateDebut().plusMonths(i - 1))
                    .statut("UNPAID")
                    .build();

            payments.add(p);
        }

        return paiementLocationRepository.saveAll(payments);
    }

    @Override
    public List<PaiementLocation> getPaymentsByProposition(Long propositionId) {
        return paiementLocationRepository.findByPropositionIdOrderByMoisNumeroAsc(propositionId);
    }

    @Override
    public List<PaiementLocation> getPaymentsByLocataire(Long locataireId) {
        return paiementLocationRepository.findByLocataireIdOrderByDateEcheanceAsc(locataireId);
    }

    @Override
    public Map<String, String> createSetupSession(Long propositionId) throws StripeException {

        List<PaiementLocation> payments =
                paiementLocationRepository.findByPropositionIdOrderByMoisNumeroAsc(propositionId);

        if (payments.isEmpty()) {
            throw new RuntimeException("No rental payment plan found for proposition " + propositionId);
        }

        PaiementLocation firstPayment = payments.get(0);

        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.SETUP)
                .setCurrency("eur")
                .setCustomerCreation(SessionCreateParams.CustomerCreation.ALWAYS)
                .setSuccessUrl("http://localhost:4200/marketplace/my-rental-proposals?autoPayment=success")
                .setCancelUrl("http://localhost:4200/marketplace/my-rental-proposals?autoPayment=cancel")
                .putMetadata("type", "RENTAL_SETUP")
                .putMetadata("propositionId", String.valueOf(propositionId))
                .putMetadata("locataireId", String.valueOf(firstPayment.getLocataireId()))
                .build();

        Session session = Session.create(params);

        Map<String, String> response = new HashMap<>();
        response.put("sessionId", session.getId());
        response.put("url", session.getUrl());

        return response;
    }

    @Override
    public void activateAutoPayment(Long propositionId, String customerId, String paymentMethodId) {

        List<PaiementLocation> payments =
                paiementLocationRepository.findByPropositionIdOrderByMoisNumeroAsc(propositionId);

        if (payments.isEmpty()) {
            throw new RuntimeException("No rental payments found for proposition " + propositionId);
        }

        for (PaiementLocation p : payments) {
            p.setStripeCustomerId(customerId);
            p.setStripePaymentMethodId(paymentMethodId);
        }

        paiementLocationRepository.saveAll(payments);
    }

    @Override
    @Scheduled(cron = "0 * * * * *")
    public void chargeDuePayments() {

        List<PaiementLocation> duePayments =
                paiementLocationRepository.findByStatutAndDateEcheanceLessThanEqual("UNPAID", LocalDate.now());

        for (PaiementLocation p : duePayments) {
            try {
                if (p.getStripeCustomerId() == null || p.getStripePaymentMethodId() == null) {
                    continue;
                }

                long amountInCents = Math.round(p.getMontant() * 100);

                PaymentIntentCreateParams params =
                        PaymentIntentCreateParams.builder()
                                .setAmount(amountInCents)
                                .setCurrency("eur")
                                .setCustomer(p.getStripeCustomerId())
                                .setPaymentMethod(p.getStripePaymentMethodId())
                                .setConfirm(true)
                                .setOffSession(true)
                                .putMetadata("type", "RENTAL_MONTHLY_PAYMENT")
                                .putMetadata("paiementLocationId", String.valueOf(p.getId()))
                                .putMetadata("propositionId", String.valueOf(p.getPropositionId()))
                                .build();

                PaymentIntent paymentIntent = PaymentIntent.create(params);

                p.setStripePaymentIntentId(paymentIntent.getId());
                p.setStatut("PAID");
                p.setDatePaiement(LocalDateTime.now());

            } catch (Exception e) {
                p.setStatut("FAILED");
            }

            paiementLocationRepository.save(p);
        }
    }
}