package org.exemple.gestionformation.serviceImpl;



import lombok.RequiredArgsConstructor;
import org.exemple.gestionformation.entity.Formation;
import org.exemple.gestionformation.entity.Module;
import org.exemple.gestionformation.entity.Ressource;
import org.exemple.gestionformation.repository.FormationRepository;
import org.exemple.gestionformation.repository.ModuleRepository;
import org.exemple.gestionformation.repository.RessourceRepository;
import org.exemple.gestionformation.service.RessourceService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RessourceServiceImpl implements RessourceService {

    private final RessourceRepository ressourceRepository;
    private final FormationRepository formationRepository;
    private final ModuleRepository moduleRepository;

    private static final Set<String> ALLOWED_RESOURCE_EXTENSIONS = Set.of(".pdf");

    @Value("${app.resource-upload.dir:uploads/resources}")
    private String resourceUploadDir;

    public Ressource create(Long formationId, Ressource ressource) {
        Formation formation = formationRepository.findById(formationId)
                .orElseThrow(() -> new RuntimeException("Formation not found with id: " + formationId));

        ressource.setFormation(formation);
        return ressourceRepository.save(ressource);
    }

    public Ressource createForModule(Long moduleId, Ressource ressource) {
        Module module = moduleRepository.findById(moduleId)
                .orElseThrow(() -> new RuntimeException("Module not found with id: " + moduleId));

        ressource.setModule(module);
        ressource.setFormation(module.getFormation());
        return ressourceRepository.save(ressource);
    }

    public List<Ressource> getAll() {
        return ressourceRepository.findAll();
    }

    public Ressource getById(Long id) {
        return ressourceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ressource not found with id: " + id));
    }

    public List<Ressource> getByFormation(Long formationId) {
        return ressourceRepository.findByFormationIdFormation(formationId);
    }

    public List<Ressource> getByModule(Long moduleId) {
        return ressourceRepository.findByModuleIdModule(moduleId);
    }

    public Ressource update(Long id, Ressource newData) {
        Ressource ressource = getById(id);
        ressource.setTitre(newData.getTitre());
        ressource.setType(newData.getType());
        ressource.setUrl(newData.getUrl());
        return ressourceRepository.save(ressource);
    }

    public void delete(Long id) {
        Ressource ressource = getById(id);
        ressourceRepository.delete(ressource);
    }

    public String uploadResource(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Resource file is empty");
        }

        String contentType = file.getContentType();
        String originalFilename = file.getOriginalFilename();
        String extension = getSafeResourceExtension(originalFilename);
        if (!".pdf".equals(extension) || contentType == null || !contentType.toLowerCase(Locale.ROOT).contains("pdf")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only PDF files are allowed");
        }

        try {
            Path uploadPath = Paths.get(resourceUploadDir).toAbsolutePath().normalize();
            Files.createDirectories(uploadPath);

            String filename = UUID.randomUUID().toString() + extension;
            Path filePath = uploadPath.resolve(filename).normalize();
            if (!filePath.startsWith(uploadPath)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid resource filename");
            }

            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            return ServletUriComponentsBuilder.fromCurrentContextPath()
                    .path("/uploads/resources/")
                    .path(filename)
                    .toUriString();
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to upload resource", e);
        }
    }

    private String getSafeResourceExtension(String originalFilename) {
        if (originalFilename != null && originalFilename.contains(".")) {
            String extension = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase(Locale.ROOT);
            if (ALLOWED_RESOURCE_EXTENSIONS.contains(extension)) {
                return extension;
            }
        }

        return "";
    }
}
