package org.example.gestioninventaire.services;

import org.example.gestioninventaire.exceptions.BadRequestException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;

@Service
public class ChatAttachmentStorageService {

    private static final Set<String> BLOCKED_MIME_PREFIXES = Set.of("application/x-msdownload", "application/x-sh");
    private final Path uploadRoot;
    private final String contextPath;

    public ChatAttachmentStorageService(
            @Value("${app.chat.upload-dir:chat-uploads}") String uploadDir,
            @Value("${server.servlet.context-path:}") String contextPath) {
        this.uploadRoot = Paths.get(uploadDir).toAbsolutePath().normalize();
        this.contextPath = contextPath == null ? "" : contextPath;
    }

    public StoredAttachment store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Le fichier est obligatoire");
        }
        String mimeType = normalizeMimeType(file.getContentType());
        if (BLOCKED_MIME_PREFIXES.contains(mimeType)) {
            throw new BadRequestException("Type de fichier non autorisé");
        }
        String originalName = cleanFileName(file.getOriginalFilename());
        String extension = getExtension(originalName);
        String storedName = UUID.randomUUID() + extension;
        Path target = uploadRoot.resolve(storedName).normalize();
        if (!target.startsWith(uploadRoot)) {
            throw new BadRequestException("Nom de fichier invalide");
        }

        try {
            Files.createDirectories(uploadRoot);
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new BadRequestException("Impossible d'enregistrer le fichier");
        }

        String type = detectMessageType(mimeType);
        String url = contextPath + "/chat-uploads/" + storedName;
        return new StoredAttachment(type, url, originalName, mimeType, file.getSize());
    }

    private String detectMessageType(String mimeType) {
        if (mimeType.startsWith("image/")) return "IMAGE";
        if (mimeType.startsWith("audio/")) return "AUDIO";
        return "FILE";
    }

    private String normalizeMimeType(String mimeType) {
        return (mimeType == null || mimeType.isBlank()) ? "application/octet-stream" : mimeType;
    }

    private String cleanFileName(String name) {
        if (name == null || name.isBlank()) return "fichier";
        return name.replace("\\", "_").replace("/", "_");
    }

    private String getExtension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        if (dot < 0 || dot == fileName.length() - 1) return "";
        return fileName.substring(dot);
    }

    public record StoredAttachment(
            String messageType,
            String url,
            String fileName,
            String mimeType,
            Long size
    ) {}
}
