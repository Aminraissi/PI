package org.example.gestioninventaire.services;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.example.gestioninventaire.dtos.CampaignAnimalDTO;
import org.example.gestioninventaire.dtos.VaccinationCampaignDTO;
import org.example.gestioninventaire.entities.*;
import org.example.gestioninventaire.enums.MovementType;
import org.example.gestioninventaire.enums.Reason;
import org.example.gestioninventaire.exceptions.BadRequestException;
import org.example.gestioninventaire.exceptions.ResourceNotFoundException;
import org.example.gestioninventaire.mappers.VaccinationMapper;
import org.example.gestioninventaire.repositories.*;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class VaccinationService {

    private final VaccinationCampaignRepository campaignRepo;
    private final VaccinationRepository vaccinationRepo;
    private final AnimalRepository animalRepo;
    private final InventoryProductRepository productRepository;
    private final StockMovementRepository stockMovementRepository;
    private final VaccinationMapper mapper;

    public VaccinationCampaign createCampaign(VaccinationCampaign campaign, String vaccin, Double dose) {

        if (campaign.getOwnerId() == null) {
            throw new IllegalArgumentException("ownerId est obligatoire pour créer une campagne");
        }

        // Charger le produit vaccin depuis l'inventaire
        InventoryProduct product = productRepository.findById(campaign.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Produit vaccin non trouvé"));

        // Vérifier que le produit appartient à cet agriculteur
        if (!product.getOwnerId().equals(campaign.getOwnerId())) {
            throw new BadRequestException("Ce produit n'appartient pas à cet agriculteur");
        }

        // Récupérer les animaux ciblés
        List<Animal> animals = animalRepo.findByOwnerIdAndEspeceAndAgeBetween(
                campaign.getOwnerId(),
               campaign.getEspece().trim().toLowerCase(),
                campaign.getAgeMin(),
                campaign.getAgeMax()
        );

        if (animals.isEmpty()) {
            throw new BadRequestException(
                    "Aucun animal de l'espèce '" + campaign.getEspece() +
                            "' trouvé pour cet agriculteur dans la tranche d'âge spécifiée"
            );
        }

        // Vérifier stock suffisant pour TOUS les animaux → bloquer sinon
        double totalDoseRequise = dose * animals.size();
        if (product.getCurrentQuantity() < totalDoseRequise) {
            throw new BadRequestException(
                    "Stock insuffisant pour cette campagne. " +
                            "Animaux ciblés : " + animals.size() +
                            ", dose par animal : " + dose + " " + product.getUnit() +
                            ", total requis : " + totalDoseRequise + " " + product.getUnit() +
                            ", disponible : " + product.getCurrentQuantity() + " " + product.getUnit()
            );
        }

        // Sauvegarder la campagne
        campaign.setStatus("PLANNED");
        campaign.setDose(dose);
        VaccinationCampaign saved = campaignRepo.save(campaign);

        // Déduire le stock total d'un coup
        product.setCurrentQuantity(product.getCurrentQuantity() - totalDoseRequise);
        productRepository.save(product);

        // Créer un StockMovement de type VACCINATION pour la campagne
        StockMovement movement = StockMovement.builder()
                .movementType(MovementType.OUT)
                .quantity(totalDoseRequise)
                .dateMouvement(LocalDateTime.now())
                .reason(Reason.VACCINATION)
                .note("Campagne vaccination #" + saved.getId() +
                        " - " + campaign.getEspece() +
                        " (" + animals.size() + " animaux × " + dose + " " + product.getUnit() + ")")
                .product(product)
                .ownerId(campaign.getOwnerId())
                .build();
        stockMovementRepository.save(movement);

        // Créer une Vaccination PENDING par animal
        for (Animal a : animals) {
            Vaccination v = Vaccination.builder()
                    .animal(a)
                    .campaign(saved)
                    .product(product)
                    .vaccin(product.getNom())
                    .dose(dose)
                    .status("PENDING")
                    .build();
            vaccinationRepo.save(v);
        }

        saved.setStatus("IN_PROGRESS");
        return saved;
    }

    // Vacciner tous → marquer DONE (stock déjà déduit à la création)
    public void vaccinateAll(Long campaignId) {
        List<Vaccination> list = vaccinationRepo.findByCampaignId(campaignId);
        for (Vaccination v : list) {
            v.setStatus("DONE");
            v.setDateVaccin(LocalDate.now());
        }
        VaccinationCampaign c = campaignRepo.findById(campaignId).get();
        c.setStatus("COMPLETED");
    }

    // Vacciner un seul animal → marquer DONE
    public void vaccinateOne(Long vaccinationId) {
        Vaccination v = vaccinationRepo.findById(vaccinationId).get();
        v.setStatus("DONE");
        v.setDateVaccin(LocalDate.now());
    }

    // Progression de la campagne
    public double getProgress(Long campaignId) {
        long total = vaccinationRepo.countByCampaignId(campaignId);
        long done  = vaccinationRepo.countByCampaignIdAndStatus(campaignId, "DONE");
        if (total == 0) return 0;
        return (double) done / total * 100;
    }

    public List<VaccinationCampaignDTO> getAllCampaigns() {
        return campaignRepo.findAll().stream().map(mapper::toDTO).toList();
    }

    public VaccinationCampaignDTO getCampaignById(Long id) {
        VaccinationCampaign c = campaignRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Campagne non trouvée"));
        return mapper.toDTO(c);
    }

    public List<CampaignAnimalDTO> getAnimalsByCampaign(Long campaignId) {
        return vaccinationRepo.findByCampaignId(campaignId)
                .stream().map(mapper::toCampaignAnimalDTO).toList();
    }
}
