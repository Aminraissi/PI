
package org.example.gestioninventaire.services;

import lombok.RequiredArgsConstructor;
import org.example.gestioninventaire.dtos.request.CreateVaccinationRequest;
import org.example.gestioninventaire.dtos.request.UpdateVaccinationRequest;
import org.example.gestioninventaire.dtos.response.VaccinationResponse;
import org.example.gestioninventaire.entities.*;
import org.example.gestioninventaire.enums.MovementType;
import org.example.gestioninventaire.enums.Reason;
import org.example.gestioninventaire.exceptions.BadRequestException;
import org.example.gestioninventaire.exceptions.ResourceNotFoundException;
import org.example.gestioninventaire.mappers.MedicalMapper;
import org.example.gestioninventaire.repositories.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class VaccinationCrudService {

    private final VaccinationRepository vaccinationRepository;
    private final AnimalRepository animalRepository;
    private final InventoryProductRepository productRepository;
    private final StockMovementRepository stockMovementRepository;
    private final MedicalMapper medicalMapper;

    @Transactional
    public VaccinationResponse create(CreateVaccinationRequest request) {
        Animal animal = animalRepository.findById(request.getAnimalId())
                .orElseThrow(() -> new ResourceNotFoundException("Animal non trouvé"));

        InventoryProduct product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Produit vaccin non trouvé"));

        // Vérifier stock suffisant
        if (product.getCurrentQuantity() < request.getDose()) {
            throw new BadRequestException(
                    "Stock insuffisant pour ce vaccin. Disponible : " +
                            product.getCurrentQuantity() + " " + product.getUnit() +
                            ", requis : " + request.getDose() + " " + product.getUnit()
            );
        }

        // Déduire la dose du stock
        product.setCurrentQuantity(product.getCurrentQuantity() - request.getDose());
        productRepository.save(product);

        // Créer le mouvement de stock automatiquement
        StockMovement movement = StockMovement.builder()
                .movementType(MovementType.OUT)
                .quantity(request.getDose())
                .dateMouvement(LocalDateTime.now())
                .reason(Reason.VACCINATION)
                .note("Vaccination animal #" + animal.getId() +
                        " (" + animal.getEspece() + " - " + animal.getReference() + ")")
                .product(product)
                .ownerId(animal.getOwnerId())
                .build();
        stockMovementRepository.save(movement);

        // Créer la vaccination liée au produit
        Vaccination vaccination = Vaccination.builder()
                .vaccin(product.getNom())
                .dateVaccin(request.getDateVaccin())
                .dose(request.getDose())
                .animal(animal)
                .product(product)
                .status("DONE")
                .build();

        vaccinationRepository.save(vaccination);
        return medicalMapper.toVaccinationResponse(vaccination);
    }

    public VaccinationResponse update(Long id, UpdateVaccinationRequest request) {
        Vaccination vaccination = vaccinationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vaccination non trouvée"));

        vaccination.setVaccin(request.getVaccin());
        vaccination.setDateVaccin(request.getDateVaccin());
        vaccination.setDose(request.getDose());

        vaccinationRepository.save(vaccination);
        return medicalMapper.toVaccinationResponse(vaccination);
    }

    public VaccinationResponse getById(Long id) {
        Vaccination vaccination = vaccinationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vaccination non trouvée"));
        return medicalMapper.toVaccinationResponse(vaccination);
    }

    public List<VaccinationResponse> getAll() {
        return vaccinationRepository.findAll()
                .stream().map(medicalMapper::toVaccinationResponse).toList();
    }

    public List<VaccinationResponse> getByAnimal(Long animalId) {
        return vaccinationRepository.findByAnimalId(animalId)
                .stream().map(medicalMapper::toVaccinationResponse).toList();
    }

    public void delete(Long id) {
        Vaccination vaccination = vaccinationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vaccination non trouvée"));
        vaccinationRepository.delete(vaccination);
    }
}
