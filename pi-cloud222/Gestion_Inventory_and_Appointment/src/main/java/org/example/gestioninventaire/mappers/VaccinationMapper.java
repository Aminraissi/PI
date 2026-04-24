package org.example.gestioninventaire.mappers;

import org.example.gestioninventaire.dtos.CampaignAnimalDTO;
import org.example.gestioninventaire.dtos.VaccinationCampaignDTO;
import org.example.gestioninventaire.dtos.VaccinationDTO;
import org.example.gestioninventaire.entities.Vaccination;
import org.example.gestioninventaire.entities.VaccinationCampaign;
import org.example.gestioninventaire.repositories.VaccinationRepository;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Component
@RequiredArgsConstructor
public class VaccinationMapper {

    private final VaccinationRepository vaccinationRepository;

    // Campaign → DTO
    public VaccinationCampaignDTO toDTO(VaccinationCampaign c) {
        VaccinationCampaignDTO dto = new VaccinationCampaignDTO();
        dto.setId(c.getId());
        dto.setEspece(c.getEspece());
        dto.setAgeMin(c.getAgeMin());
        dto.setAgeMax(c.getAgeMax());
        dto.setPlannedDate(c.getPlannedDate());
        dto.setStatus(c.getStatus());
        dto.setOwnerId(c.getOwnerId());
        dto.setProductId(c.getProductId());
        dto.setDose(c.getDose());

        // Récupérer nom du produit et dose depuis la première Vaccination liée
        List<Vaccination> vaccinations = vaccinationRepository.findByCampaignId(c.getId());

        if (!vaccinations.isEmpty()) {
            Vaccination v = vaccinations.get(0);

            if (v.getProduct() != null) {
                dto.setProductName(v.getProduct().getNom());
                dto.setProductId(v.getProduct().getId());
            }

            dto.setDose(v.getDose());
        }

        return dto;
    }

    // DTO → Campaign
    public VaccinationCampaign toEntity(VaccinationCampaignDTO dto) {
        return VaccinationCampaign.builder()
                .id(dto.getId())
                .espece(dto.getEspece())
                .ageMin(dto.getAgeMin())
                .ageMax(dto.getAgeMax())
                .plannedDate(dto.getPlannedDate())
                .status(dto.getStatus())
                .ownerId(dto.getOwnerId())
                .productId(dto.getProductId())
                .dose(dto.getDose())
                .build();
    }

    // Vaccination → VaccinationDTO
    public VaccinationDTO toDTO(Vaccination v) {
        VaccinationDTO dto = new VaccinationDTO();
        dto.setId(v.getId());
        dto.setAnimalId(v.getAnimal() != null ? v.getAnimal().getId() : null);
        dto.setCampaignId(v.getCampaign() != null ? v.getCampaign().getId() : null);
        dto.setPlannedDate(
                v.getCampaign() != null ? v.getCampaign().getPlannedDate() : null
        );
        dto.setDateVaccin(v.getDateVaccin());
        dto.setDose(v.getDose());
        dto.setStatus(v.getStatus());
        return dto;
    }

    // Vaccination → CampaignAnimalDTO
    public CampaignAnimalDTO toCampaignAnimalDTO(Vaccination v) {
        CampaignAnimalDTO dto = new CampaignAnimalDTO();
        dto.setVaccinationId(v.getId());
        if (v.getAnimal() != null) {
            dto.setAnimalId(v.getAnimal().getId());
            dto.setAnimalName(v.getAnimal().getEspece() + " - " + v.getAnimal().getReference());
        }
        dto.setStatus(v.getStatus());
        dto.setDateVaccin(v.getDateVaccin());
        return dto;
    }
}
