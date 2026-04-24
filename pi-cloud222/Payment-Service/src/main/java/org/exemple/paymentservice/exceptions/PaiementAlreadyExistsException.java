package org.exemple.paymentservice.exceptions;

/**
 * Exception thrown when a Paiement already exists for a Facture
 */
public class PaiementAlreadyExistsException extends RuntimeException {
    public PaiementAlreadyExistsException(String message) {
        super(message);
    }

    public PaiementAlreadyExistsException(String message, Throwable cause) {
        super(message, cause);
    }
}

