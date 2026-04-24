package org.example.gestioninventaire.mappers;


import lombok.RequiredArgsConstructor;
import org.example.gestioninventaire.dtos.request.AnimalDetailResponse;
import org.example.gestioninventaire.dtos.response.HealthRecordResponse;
import org.example.gestioninventaire.dtos.response.UserSummaryResponse;
import org.example.gestioninventaire.dtos.response.VaccinationResponse;
import org.example.gestioninventaire.entities.Animal;
import org.example.gestioninventaire.entities.HealthRecord;
import org.example.gestioninventaire.entities.Vaccination;
import org.example.gestioninventaire.feigns.UserClient;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class MedicalMapper {

    private final AnimalMapper animalMapper;
    private final UserMapper userMapper;
    private final UserClient userClient;

    public HealthRecordResponse toHealthRecordResponse(HealthRecord record) {
        if (record == null) return null;

        return HealthRecordResponse.builder()
                .id(record.getId())
                .maladie(record.getMaladie())
                .traitement(record.getTraitement())
                .dateH(record.getDateH())
                .animal(animalMapper.toSummary(record.getAnimal()))
                .build();
    }

    public VaccinationResponse toVaccinationResponse(Vaccination vaccination) {
        if (vaccination == null) return null;

        return VaccinationResponse.builder()
                .id(vaccination.getId())
                .vaccin(vaccination.getVaccin())
                .productId(vaccination.getProduct() != null ? vaccination.getProduct().getId() : null)
                .dateVaccin(vaccination.getDateVaccin())
                .dose(vaccination.getDose())
                .status(vaccination.getStatus())
                .animal(animalMapper.toSummary(vaccination.getAnimal()))
                .build();
    }

    public AnimalDetailResponse toAnimalDetailResponse(
            Animal animal,
            List<HealthRecord> healthRecords,
            List<Vaccination> vaccinations
    ) {
        UserSummaryResponse owner = null;
        if (animal.getOwnerId() != null) {
            try {
                owner = userMapper.toSummary(userClient.getUserById(animal.getOwnerId()));
            } catch (Exception e) {
                owner = null;
            }
        }

        return AnimalDetailResponse.builder()
                .id(animal.getId())
                .espece(animal.getEspece())
                .poids(animal.getPoids())
                .reference(animal.getReference())
                .dateNaissance(animal.getDateNaissance())
                .owner(owner)
                .healthRecords(healthRecords.stream().map(this::toHealthRecordResponse).toList())
                .vaccinations(vaccinations.stream().map(this::toVaccinationResponse).toList())
                .build();
    }
}
