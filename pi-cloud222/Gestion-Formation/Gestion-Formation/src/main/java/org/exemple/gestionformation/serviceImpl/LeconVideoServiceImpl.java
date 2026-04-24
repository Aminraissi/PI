package org.exemple.gestionformation.serviceImpl;




import lombok.RequiredArgsConstructor;
import org.exemple.gestionformation.entity.LeconVideo;
import org.exemple.gestionformation.entity.Module;
import org.exemple.gestionformation.repository.LeconVideoRepository;
import org.exemple.gestionformation.repository.ModuleRepository;
import org.exemple.gestionformation.service.LeconVideoService;
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
public class LeconVideoServiceImpl implements LeconVideoService {

    private final LeconVideoRepository leconVideoRepository;
    private final ModuleRepository moduleRepository;

    private static final Set<String> ALLOWED_VIDEO_EXTENSIONS = Set.of(".mp4", ".webm", ".mov", ".avi", ".mkv");

    @Value("${app.video-upload.dir:uploads/videos}")
    private String videoUploadDir;

    public LeconVideo create(Long moduleId, LeconVideo leconVideo) {
        Module module = moduleRepository.findById(moduleId)
                .orElseThrow(() -> new RuntimeException("Module not found with id: " + moduleId));

        leconVideo.setModule(module);
        return leconVideoRepository.save(leconVideo);
    }

    public List<LeconVideo> getAll() {
        return leconVideoRepository.findAll();
    }

    public LeconVideo getById(Long id) {
        return leconVideoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Lecon not found with id: " + id));
    }

    public List<LeconVideo> getByModule(Long moduleId) {
        return leconVideoRepository.findByModuleIdModule(moduleId);
    }

    public LeconVideo update(Long id, LeconVideo newData) {
        LeconVideo lecon = getById(id);
        lecon.setTitre(newData.getTitre());
        lecon.setUrlVideo(newData.getUrlVideo());
        lecon.setDureeSecondes(newData.getDureeSecondes());
        lecon.setOrdre(newData.getOrdre());
        lecon.setEstGratuitePreview(newData.getEstGratuitePreview());
        return leconVideoRepository.save(lecon);
    }

    public void delete(Long id) {
        LeconVideo lecon = getById(id);
        leconVideoRepository.delete(lecon);
    }

    public String uploadVideo(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Video file is empty");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.toLowerCase(Locale.ROOT).startsWith("video/")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only video files are allowed");
        }

        try {
            Path uploadPath = Paths.get(videoUploadDir).toAbsolutePath().normalize();
            Files.createDirectories(uploadPath);

            String originalFilename = file.getOriginalFilename();
            String extension = getSafeVideoExtension(originalFilename, contentType);
            String filename = UUID.randomUUID().toString() + extension;

            Path filePath = uploadPath.resolve(filename).normalize();
            if (!filePath.startsWith(uploadPath)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid video filename");
            }

            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            return ServletUriComponentsBuilder.fromCurrentContextPath()
                    .path("/uploads/videos/")
                    .path(filename)
                    .toUriString();
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to upload video", e);
        }
    }

    private String getSafeVideoExtension(String originalFilename, String contentType) {
        if (originalFilename != null && originalFilename.contains(".")) {
            String extension = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase(Locale.ROOT);
            if (ALLOWED_VIDEO_EXTENSIONS.contains(extension)) {
                return extension;
            }
        }

        return switch (contentType.toLowerCase(Locale.ROOT)) {
            case "video/webm" -> ".webm";
            case "video/quicktime" -> ".mov";
            default -> ".mp4";
        };
    }
}
