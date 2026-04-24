package org.example.gestioninventaire.services;

import lombok.RequiredArgsConstructor;
import org.example.gestioninventaire.dtos.request.AnimalDetailResponse;
import org.example.gestioninventaire.dtos.request.CreateAnimalRequest;
import org.example.gestioninventaire.dtos.request.UpdateAnimalRequest;
import org.example.gestioninventaire.dtos.response.AnimalSummaryResponse;
import org.example.gestioninventaire.entities.Animal;
import org.example.gestioninventaire.entities.Appointment;
import org.example.gestioninventaire.entities.HealthRecord;
import org.example.gestioninventaire.entities.Vaccination;
import org.example.gestioninventaire.exceptions.BadRequestException;
import org.example.gestioninventaire.exceptions.ResourceNotFoundException;
import org.example.gestioninventaire.feigns.UserClient;
import org.example.gestioninventaire.mappers.AnimalMapper;
import org.example.gestioninventaire.mappers.MedicalMapper;
import org.example.gestioninventaire.repositories.AnimalRepository;
import org.example.gestioninventaire.repositories.AppointmentRepository;
import org.example.gestioninventaire.repositories.HealthRecordRepository;
import org.example.gestioninventaire.repositories.VaccinationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AnimalCrudService {

    private final AnimalRepository animalRepository;
    private final UserClient userClient;
    private final HealthRecordRepository healthRecordRepository;
    private final VaccinationRepository vaccinationRepository;
    private final AppointmentRepository appointmentRepository;
    private final AnimalMapper animalMapper;
    private final MedicalMapper medicalMapper;

    public AnimalSummaryResponse create(CreateAnimalRequest request) {
        // Vérifier que le propriétaire existe via Feign
        try {
            userClient.getUserById(request.getOwnerId());
        } catch (Exception e) {
            throw new ResourceNotFoundException("Propriétaire non trouvé");
        }

        if (animalRepository.existsByReference(request.getReference())) {
            throw new BadRequestException("Cette référence animal existe déjà");
        }

        Animal animal = Animal.builder()
                .espece(request.getEspece())
                .poids(request.getPoids())
                .reference(request.getReference())
                .dateNaissance(request.getDateNaissance())
                .ownerId(request.getOwnerId())
                .build();

        animalRepository.save(animal);
        return animalMapper.toSummary(animal);
    }

    public AnimalSummaryResponse update(Long id, UpdateAnimalRequest request) {
        Animal animal = animalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Animal non trouvé"));

        animal.setEspece(request.getEspece());
        animal.setPoids(request.getPoids());
        animal.setReference(request.getReference());
        animal.setDateNaissance(request.getDateNaissance());

        animalRepository.save(animal);
        return animalMapper.toSummary(animal);
    }

    public AnimalSummaryResponse getById(Long id) {
        Animal animal = animalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Animal non trouvé"));
        return animalMapper.toSummary(animal);
    }

    public AnimalDetailResponse getDetailById(Long id) {
        Animal animal = animalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Animal non trouvé"));

        // Utiliser les méthodes avec JOIN FETCH pour éviter LazyInitializationException
        List<HealthRecord> healthRecords = healthRecordRepository.findByAnimalIdWithAnimal(id);
        List<Vaccination> vaccinations   = vaccinationRepository.findByAnimalId(id);

        return medicalMapper.toAnimalDetailResponse(animal, healthRecords, vaccinations);
    }

    public List<AnimalSummaryResponse> getAll() {
        // Retourne uniquement les animaux non soft-deleted
        return animalRepository.findByIsDeletedFalse()
                .stream()
                .map(animalMapper::toSummary)
                .toList();
    }

    public List<AnimalSummaryResponse> getByOwner(Long ownerId) {
        // Retourne uniquement les animaux non soft-deleted de cet agriculteur
        return animalRepository.findByOwnerIdAndIsDeletedFalse(ownerId)
                .stream()
                .map(animalMapper::toSummary)
                .toList();
    }

    @Transactional
    public void delete(Long id) {
        Animal animal = animalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Animal non trouvé"));

        // Soft delete : on marque l'animal comme supprimé
        // Les health_records, vaccinations et appointments sont préservés
        // pour que le vétérinaire garde son historique
        animal.setIsDeleted(true);
        animalRepository.save(animal);
    }
}
