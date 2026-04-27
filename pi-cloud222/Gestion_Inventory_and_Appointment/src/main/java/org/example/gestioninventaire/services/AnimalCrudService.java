package org.example.gestioninventaire.services;

import lombok.RequiredArgsConstructor;
import org.example.gestioninventaire.dtos.request.AnimalDetailResponse;
import org.example.gestioninventaire.dtos.request.CreateAnimalRequest;
import org.example.gestioninventaire.dtos.request.UpdateAnimalRequest;
import org.example.gestioninventaire.dtos.response.AnimalSummaryResponse;
import org.example.gestioninventaire.entities.Animal;
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

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class AnimalCrudService {

    private static final Pattern REFERENCE_PATTERN = Pattern.compile("^([A-Z]{3})-(\\d+)$");

    private final AnimalRepository animalRepository;
    private final UserClient userClient;
    private final HealthRecordRepository healthRecordRepository;
    private final VaccinationRepository vaccinationRepository;
    private final AppointmentRepository appointmentRepository;
    private final AnimalMapper animalMapper;
    private final MedicalMapper medicalMapper;

    public AnimalSummaryResponse create(CreateAnimalRequest request) {
        // Verifier que le proprietaire existe via Feign
        try {
            userClient.getUserById(request.getOwnerId());
        } catch (Exception e) {
            throw new ResourceNotFoundException("Proprietaire non trouve");
        }

        String generatedReference = generateReference(request.getEspece());

        Animal animal = Animal.builder()
                .espece(request.getEspece())
                .poids(request.getPoids())
                .reference(generatedReference)
                .dateNaissance(request.getDateNaissance())
                .ownerId(request.getOwnerId())
                .build();

        animalRepository.save(animal);
        return animalMapper.toSummary(animal);
    }

    public AnimalSummaryResponse update(Long id, UpdateAnimalRequest request) {
        Animal animal = animalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Animal non trouve"));

        animal.setEspece(request.getEspece());
        animal.setPoids(request.getPoids());
        animal.setDateNaissance(request.getDateNaissance());

        animalRepository.save(animal);
        return animalMapper.toSummary(animal);
    }

    public AnimalSummaryResponse getById(Long id) {
        Animal animal = animalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Animal non trouve"));
        return animalMapper.toSummary(animal);
    }

    public AnimalDetailResponse getDetailById(Long id) {
        Animal animal = animalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Animal non trouve"));

        // Utiliser les methodes avec JOIN FETCH pour eviter LazyInitializationException
        List<HealthRecord> healthRecords = healthRecordRepository.findByAnimalIdWithAnimal(id);
        List<Vaccination> vaccinations = vaccinationRepository.findByAnimalId(id);

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
                .orElseThrow(() -> new ResourceNotFoundException("Animal non trouve"));

        // Soft delete : on marque l'animal comme supprime
        // Les health_records, vaccinations et appointments sont preserves
        // pour que le veterinaire garde son historique
        animal.setIsDeleted(true);
        animalRepository.save(animal);
    }

    private String generateReference(String espece) {
        String normalized = normalizeEspece(espece);
        String prefix = normalized.length() >= 3
                ? normalized.substring(0, 3)
                : String.format("%-3s", normalized).replace(' ', 'X');

        List<String> existing = animalRepository.findReferencesByPrefix(prefix);
        int next = existing.stream()
                .map(this::extractSequence)
                .max(Integer::compareTo)
                .orElse(0) + 1;

        String candidate = formatReference(prefix, next);
        while (animalRepository.existsByReference(candidate)) {
            next++;
            candidate = formatReference(prefix, next);
        }
        return candidate;
    }

    private int extractSequence(String reference) {
        if (reference == null) return 0;
        Matcher matcher = REFERENCE_PATTERN.matcher(reference);
        if (!matcher.matches()) return 0;
        try {
            return Integer.parseInt(matcher.group(2));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private String formatReference(String prefix, int sequence) {
        return String.format("%s-%03d", prefix, sequence);
    }

    private String normalizeEspece(String espece) {
        if (espece == null || espece.isBlank()) {
            throw new BadRequestException("Espece invalide");
        }
        String noAccent = Normalizer.normalize(espece, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        String lettersOnly = noAccent.replaceAll("[^A-Za-z]", "").toUpperCase(Locale.ROOT);
        if (lettersOnly.isBlank()) {
            throw new BadRequestException("Espece invalide");
        }
        return lettersOnly;
    }
}
