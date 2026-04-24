package org.exemple.paymentservice.dtos;

import lombok.Data;

import java.time.LocalDate;

@Data
public class CreateRentalPaymentPlanRequest {

    private Long propositionId;
    private Long locationId;
    private Long locataireId;
    private Long agriculteurId;

    private Integer nbMois;
    private Double montantMensuel;
    private LocalDate dateDebut;
}