package org.example.gestioninventaire.dtos.request;

import lombok.Builder;
import lombok.Data;
import org.example.gestioninventaire.dtos.response.HealthRecordResponse;
import org.example.gestioninventaire.dtos.response.UserSummaryResponse;
import org.example.gestioninventaire.dtos.response.VaccinationResponse;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class AnimalDetailResponse {
    private Long id;
    private String espece;
    private Double poids;
    private String reference;
    private LocalDate dateNaissance;
    private UserSummaryResponse owner;
    private List<HealthRecordResponse> healthRecords;
    private List<VaccinationResponse> vaccinations;
}
