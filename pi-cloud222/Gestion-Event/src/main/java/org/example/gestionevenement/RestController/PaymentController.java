package org.example.gestionevenement.RestController;

import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Refund;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.RefundCreateParams;
import org.example.gestionevenement.Services.EmailService;
import org.example.gestionevenement.Services.IReservation;
import org.example.gestionevenement.Services.ITicket;
import org.example.gestionevenement.entities.EtatPaiement;
import org.example.gestionevenement.entities.Event;
import org.example.gestionevenement.entities.Reservation;
import org.example.gestionevenement.entities.Ticket;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/payment")
public class PaymentController {

    private final IReservation reservationService;
    private final ITicket ticketService;
    private final EmailService emailService;
    private static final String DEFAULT_EMAIL = "maalejghada@gmail.com";
    public PaymentController(IReservation reservationService, ITicket ticketService, EmailService  emailService) {
        this.reservationService = reservationService;
        this.ticketService      = ticketService;
        this.emailService       = emailService;
    }
    @PostMapping("/create-payment-intent/{reservationId}")
    public Map<String, String> createPaymentIntent(@PathVariable int reservationId) throws StripeException {

        Reservation reservation = getValidReservation(reservationId);
        long amount = toStripeAmount(reservation.getMontant());

        PaymentIntent intent = PaymentIntent.create(
                PaymentIntentCreateParams.builder()
                        .setAmount(amount)
                        .setCurrency("eur")
                        .addPaymentMethodType("card")
                        .putMetadata("reservationId", String.valueOf(reservationId))
                        .putMetadata("userId", String.valueOf(reservation.getId_user()))
                        .build()
        );
        return Map.of(
                "clientSecret", intent.getClientSecret(),
                "paymentIntentId", intent.getId()
        );
    }

    @PostMapping("/confirm-payment/{reservationId}")
    public Reservation confirmPayment(@PathVariable int reservationId, @RequestParam String paymentIntentId) throws StripeException {

        PaymentIntent intent = PaymentIntent.retrieve(paymentIntentId);

        if (!"succeeded".equals(intent.getStatus())) {
            throw new RuntimeException("Payment not confirmed: " + intent.getStatus());
        }

        Reservation reservation = getValidReservation(reservationId);

        if (reservation.getEtatPaiement() == EtatPaiement.PAID) {
            return reservation;
        }

        reservation.setEtatPaiement(EtatPaiement.PAID);
        reservation.setDateInscription(LocalDateTime.now());
        reservation.setPaymentIntentId(paymentIntentId);

        Event event = reservation.getEvenement();

        event.setInscrits(event.getInscrits() + reservation.getNbPlaceReserve());

        reservationService.updateReservation(reservation);
        Reservation updatedReservation = reservationService.updateReservation(reservation);

        try {
            Ticket ticketResult = ticketService.generateTicket(updatedReservation);

            if (ticketResult != null && ticketResult.getQrCode() != null) {

                String userEmail = getDefaultEmail((int) reservation.getId_user());
                emailService.sendTicketConfirmation(userEmail, updatedReservation, ticketResult);

            } else {
                System.out.println("Failed to generate ticket for reservation: " + reservationId);
            }

        } catch (Exception e) {
            System.err.println("Failed to generate ticket or send email: " + e.getMessage());
            e.printStackTrace();
        }

        return updatedReservation;
    }

    @DeleteMapping("/cancel-reservation/{reservationId}")
    public Map<String, String> cancelReservation(@PathVariable int reservationId) {

        Reservation reservation = getValidReservation(reservationId);

        if (reservation.getEtatPaiement() == EtatPaiement.PAID) {
            return error("Cannot cancel a paid reservation. Contact organizer.");
        }

        reservationService.removeReservation(reservationId);
        return success("Reservation cancelled successfully.");
    }

    @PostMapping("/refund-event/{eventId}")
    public Map<String, Object> refundAllParticipants(@PathVariable int eventId) throws StripeException {

        var reservations = reservationService.getReservationsByEvent(eventId);

        int refunded = 0, skipped = 0;
        var errors = new java.util.ArrayList<String>();

        for (Reservation r : reservations) {

            if (!isRefundable(r)) {
                skipped++;
                continue;
            }
            try {
                Refund.create(
                        RefundCreateParams.builder()
                                .setPaymentIntent(r.getPaymentIntentId())
                                .build()
                );
                r.setEtatPaiement(EtatPaiement.REFUNDED);
                reservationService.updateReservation(r);
                refunded++;
            } catch (StripeException e) {
                errors.add("Reservation " + r.getId() + ": " + e.getMessage());
            }
        }
        return Map.of(
                "refunded", refunded,
                "skipped", skipped,
                "errors", errors
        );
    }
    private Reservation getValidReservation(int id) {
        Reservation r = reservationService.getReservation(id);

        if (r == null) throw new RuntimeException("Reservation not found");
        if (r.getMontant() <= 0) throw new RuntimeException("Invalid amount");

        return r;
    }

    private long toStripeAmount(double amount) {
        return (long) (amount * 100);
    }

    private boolean isRefundable(Reservation r) {
        return r.getEtatPaiement() == EtatPaiement.PAID && r.getPaymentIntentId() != null;
    }

    private Map<String, String> success(String msg) {
        return Map.of("status", "success", "message", msg);
    }

    private Map<String, String> error(String msg) {
        return Map.of("status", "error", "message", msg);
    }

    private String getDefaultEmail(int userId) {
        return "maalejghada7@gmail.com";
    }
}