package org.exemple.paymentservice.dtos;

import lombok.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;

/**
 * DTO for Facture entity
 * Used for creating, updating, and retrieving invoices
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FactureDTO {

    private Long idFacture;

    @NotNull(message = "Numero cannot be null")
    private String numero;

    @NotNull(message = "Date cannot be null")
    private LocalDate date;

    @NotNull(message = "Total cannot be null")
    @Positive(message = "Total must be a positive value")
    private Double total;

    private String pdfUrl;
}

