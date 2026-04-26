package org.exemple.paymentservice.services;

import com.stripe.exception.StripeException;
import org.exemple.paymentservice.dtos.CreateRentalPaymentPlanRequest;
import org.exemple.paymentservice.entities.PaiementLocation;

import java.util.List;
import java.util.Map;

public interface RentalPaymentService {

    List<PaiementLocation> createPaymentPlan(CreateRentalPaymentPlanRequest request);

    List<PaiementLocation> getPaymentsByProposition(Long propositionId);

    List<PaiementLocation> getPaymentsByLocataire(Long locataireId);

    Map<String, String> createSetupSession(Long propositionId) throws StripeException;

    void activateAutoPayment(Long propositionId, String customerId, String paymentMethodId);

    void chargeDuePayments();
    List<PaiementLocation> getAllPayments();
}