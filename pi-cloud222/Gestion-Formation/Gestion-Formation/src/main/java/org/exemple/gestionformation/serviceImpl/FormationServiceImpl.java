package org.exemple.gestionformation.serviceImpl;


import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.exemple.gestionformation.entity.Formation;
import org.exemple.gestionformation.repository.FormationRepository;
import org.exemple.gestionformation.service.FormationService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FormationServiceImpl implements FormationService {

    private final FormationRepository formationRepository;

    private static final String UPLOAD_DIR = "uploads/images/";

    public Formation create(Formation formation) {
        if (formation.getDateCreation() == null) {
            formation.setDateCreation(LocalDate.now());
        }
        return formationRepository.save(formation);
    }

    public List<Formation> getAll() {
        return formationRepository.findAll();
    }

    public Formation getById(Long id) {
        return formationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Formation introuvable avec id = " + id));
    }

    public Formation update(Long id, Formation data) {
        Formation formation = getById(id);

        formation.setTitre(data.getTitre());
        formation.setDescription(data.getDescription());
        formation.setThematique(data.getThematique());
        formation.setNiveau(data.getNiveau());
        formation.setType(data.getType());
        formation.setPrix(data.getPrix());
        formation.setEstPayante(data.getEstPayante());
        formation.setLangue(data.getLangue());
        formation.setStatut(data.getStatut());

        return formationRepository.save(formation);
    }

    public void delete(Long id) {
        formationRepository.deleteById(id);
    }

    public String uploadImage(MultipartFile file) {
        try {
            // Create upload directory if it doesn't exist
            Path uploadPath = Paths.get(UPLOAD_DIR);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // Generate unique filename
            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename != null && originalFilename.contains(".")
                    ? originalFilename.substring(originalFilename.lastIndexOf("."))
                    : "";
            String filename = UUID.randomUUID().toString() + extension;

            // Save file
            Path filePath = uploadPath.resolve(filename);
            Files.copy(file.getInputStream(), filePath);

            // Return relative URL
            return "/uploads/images/" + filename;
        } catch (IOException e) {
            throw new RuntimeException("Failed to upload image", e);
        }
    }
}