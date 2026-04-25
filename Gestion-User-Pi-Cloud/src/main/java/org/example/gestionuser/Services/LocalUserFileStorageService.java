package org.example.gestionuser.Services;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@Service
public class LocalUserFileStorageService {

    private final Path uploadRoot;

    public LocalUserFileStorageService() throws IOException {
        this.uploadRoot = Path.of("uploads", "user-documents").toAbsolutePath().normalize();
        Files.createDirectories(this.uploadRoot);
    }

    public String store(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is required");
        }

        String originalName = StringUtils.cleanPath(file.getOriginalFilename() == null ? "document" : file.getOriginalFilename());
        String safeName = originalName.replaceAll("[^a-zA-Z0-9._-]", "-");
        String storedName = System.currentTimeMillis() + "-" + safeName;
        Path target = this.uploadRoot.resolve(storedName).normalize();

        if (!target.startsWith(this.uploadRoot)) {
            throw new IllegalArgumentException("Invalid file name");
        }

        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
        }

        return "/user/uploads/" + storedName;
    }

    public String getUploadRootLocation() {
        return this.uploadRoot.toUri().toString();
    }
}
