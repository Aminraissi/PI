package org.example.gestioninventaire.services;

import lombok.RequiredArgsConstructor;
import org.example.gestioninventaire.dtos.request.CreateHealthRecordRequest;
import org.example.gestioninventaire.dtos.request.UpdateHealthRecordRequest;
import org.example.gestioninventaire.dtos.response.HealthRecordResponse;
import org.example.gestioninventaire.entities.Animal;
import org.example.gestioninventaire.entities.HealthRecord;
import org.example.gestioninventaire.exceptions.ResourceNotFoundException;
import org.example.gestioninventaire.mappers.MedicalMapper;
import org.example.gestioninventaire.repositories.AnimalRepository;
import org.example.gestioninventaire.repositories.HealthRecordRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class HealthRecordCrudService {

    private final HealthRecordRepository healthRecordRepository;
    private final AnimalRepository animalRepository;
    private final MedicalMapper medicalMapper;

    public HealthRecordResponse create(CreateHealthRecordRequest request) {
        Animal animal = animalRepository.findById(request.getAnimalId())
                .orElseThrow(() -> new ResourceNotFoundException("Animal non trouvé"));

        HealthRecord record = HealthRecord.builder()
                .maladie(request.getMaladie())
                .traitement(request.getTraitement())
                .dateH(request.getDateH())
                .animal(animal)
                .build();

        healthRecordRepository.save(record);
        // Recharger avec JOIN FETCH pour éviter LazyInitializationException
        HealthRecord saved = healthRecordRepository.findByIdWithAnimal(record.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Dossier santé non trouvé"));
        return medicalMapper.toHealthRecordResponse(saved);
    }

    public HealthRecordResponse update(Long id, UpdateHealthRecordRequest request) {
        HealthRecord record = healthRecordRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Dossier santé non trouvé"));

        record.setMaladie(request.getMaladie());
        record.setTraitement(request.getTraitement());
        record.setDateH(request.getDateH());

        healthRecordRepository.save(record);
        // Recharger avec JOIN FETCH pour éviter LazyInitializationException
        HealthRecord updated = healthRecordRepository.findByIdWithAnimal(id)
                .orElseThrow(() -> new ResourceNotFoundException("Dossier santé non trouvé"));
        return medicalMapper.toHealthRecordResponse(updated);
    }

    public HealthRecordResponse getById(Long id) {
        HealthRecord record = healthRecordRepository.findByIdWithAnimal(id)
                .orElseThrow(() -> new ResourceNotFoundException("Dossier santé non trouvé"));
        return medicalMapper.toHealthRecordResponse(record);
    }

    public List<HealthRecordResponse> getAll() {
        return healthRecordRepository.findAllWithAnimal()
                .stream()
                .map(medicalMapper::toHealthRecordResponse)
                .toList();
    }

    public List<HealthRecordResponse> getByAnimal(Long animalId) {
        return healthRecordRepository.findByAnimalIdWithAnimal(animalId)
                .stream()
                .map(medicalMapper::toHealthRecordResponse)
                .toList();
    }

    public void delete(Long id) {
        HealthRecord record = healthRecordRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Dossier santé non trouvé"));
        healthRecordRepository.delete(record);
    }
}
