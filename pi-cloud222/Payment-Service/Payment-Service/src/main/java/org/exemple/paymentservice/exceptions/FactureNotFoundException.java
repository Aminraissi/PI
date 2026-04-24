package org.exemple.paymentservice.exceptions;

/**
 * Exception thrown when a Facture is not found
 */
public class FactureNotFoundException extends RuntimeException {
    public FactureNotFoundException(String message) {
        super(message);
    }

    public FactureNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public FactureNotFoundException(Long idFacture) {
        super("Facture not found with id: " + idFacture);
    }
}

