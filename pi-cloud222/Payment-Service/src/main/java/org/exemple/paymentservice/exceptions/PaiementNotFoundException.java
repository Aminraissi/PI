package org.exemple.paymentservice.exceptions;

/**
 * Exception thrown when a Paiement is not found
 */
public class PaiementNotFoundException extends RuntimeException {
    public PaiementNotFoundException(String message) {
        super(message);
    }

    public PaiementNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public PaiementNotFoundException(Long idPaiement) {
        super("Paiement not found with id: " + idPaiement);
    }
}

