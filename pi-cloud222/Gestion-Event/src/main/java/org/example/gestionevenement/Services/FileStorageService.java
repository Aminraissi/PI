package org.example.gestionevenement.Services;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@Service
public class FileStorageService {

    private final String uploadDir =
            "C:/Users/USER/Desktop/finalproject/PI/pi-cloud222/Gestion-Event/uploads/";

    public String saveFile(MultipartFile file) {

        if (file == null || file.isEmpty()) {
            return null;
        }

        try {
            Files.createDirectories(Paths.get(uploadDir));

            String fileName =
                    java.util.UUID.randomUUID()
                            + "_" + file.getOriginalFilename();

            Path path = Paths.get(uploadDir).resolve(fileName);

            Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);

            return fileName;

        } catch (Exception e) {
            throw new RuntimeException("File upload failed", e);
        }
    }

    public Path loadFile(String filename) {
        return Paths.get(uploadDir).resolve(filename).normalize();
    }
}
