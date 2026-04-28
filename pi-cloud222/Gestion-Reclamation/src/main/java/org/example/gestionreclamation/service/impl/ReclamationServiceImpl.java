package org.example.gestionreclamation.service.impl;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.example.gestionreclamation.dto.*;
import org.example.gestionreclamation.entity.Reclamation;
import org.example.gestionreclamation.entity.ReclamationMessage;
import org.example.gestionreclamation.enums.ReclamationStatus;
import org.example.gestionreclamation.feign.UserClient;
import org.example.gestionreclamation.repository.ReclamationMessageRepository;
import org.example.gestionreclamation.repository.ReclamationRepository;
import org.example.gestionreclamation.service.ReclamationService;
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReclamationServiceImpl implements ReclamationService {

    private final ReclamationRepository reclamationRepository;
    private final ReclamationMessageRepository messageRepository;
    private final UserClient userClient;

    @Value("${reclamation.upload-dir:uploads/reclamations}")
    private String uploadDir;

    @Value("${server.servlet.context-path:}")
    private String contextPath;

    @Override
    public ReclamationResponse create(CreateReclamationRequest request) {
        return create(request, null);
    }

    @Override
    public ReclamationResponse create(CreateReclamationRequest request, MultipartFile attachment) {
        UserSummaryDto user = getUserOrThrow(request.userId());
        StoredAttachment storedAttachment = storeAttachment(attachment);

        Reclamation reclamation = Reclamation.builder()
                .userId(request.userId())
                .subject(request.subject())
                .category(request.category())
                .description(request.description())
                .priority(request.priority())
                .attachmentUrl(storedAttachment == null ? null : storedAttachment.url())
                .attachmentFileName(storedAttachment == null ? null : storedAttachment.originalName())
                .build();

        reclamation = reclamationRepository.save(reclamation);

        ReclamationMessage initialMessage = ReclamationMessage.builder()
                .reclamation(reclamation)
                .senderId(request.userId())
                .senderName(user.fullName().isBlank() ? user.email() : user.fullName())
                .senderRole(org.example.gestionreclamation.enums.SenderRole.USER)
                .message(request.description())
                .build();
        messageRepository.save(initialMessage);

        return mapToResponse(reclamationRepository.findById(reclamation.getId()).orElseThrow(), user);
    }

    @Override
    public List<ReclamationResponse> getAll(ReclamationStatus status) {
        List<Reclamation> list = status == null
                ? reclamationRepository.findAllByOrderByCreatedAtDesc()
                : reclamationRepository.findByStatusOrderByCreatedAtDesc(status);

        return list.stream().map(r -> mapToResponse(r, getUserOrThrow(r.getUserId()))).toList();
    }

    @Override
    public List<ReclamationResponse> getByUser(Long userId) {
        UserSummaryDto user = getUserOrThrow(userId);
        return reclamationRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(r -> mapToResponse(r, user))
                .toList();
    }

    @Override
    public ReclamationResponse getById(Long id) {
        Reclamation reclamation = findReclamation(id);
        return mapToResponse(reclamation, getUserOrThrow(reclamation.getUserId()));
    }

    @Override
    public ReclamationResponse addMessage(Long reclamationId, AddMessageRequest request) {
        Reclamation reclamation = findReclamation(reclamationId);
        UserSummaryDto sender = getUserOrThrow(request.senderId());

        ReclamationMessage message = ReclamationMessage.builder()
                .reclamation(reclamation)
                .senderId(request.senderId())
                .senderName(sender.fullName().isBlank() ? sender.email() : sender.fullName())
                .senderRole(request.senderRole())
                .message(request.message())
                .build();
        messageRepository.save(message);

        if (request.senderRole() == org.example.gestionreclamation.enums.SenderRole.ADMIN && reclamation.getStatus() == ReclamationStatus.EN_ATTENTE) {
            reclamation.setStatus(ReclamationStatus.EN_COURS);
        }
        reclamationRepository.save(reclamation);

        return mapToResponse(findReclamation(reclamationId), getUserOrThrow(reclamation.getUserId()));
    }

    @Override
    public ReclamationResponse updateStatus(Long reclamationId, UpdateStatusRequest request) {
        Reclamation reclamation = findReclamation(reclamationId);
        reclamation.setStatus(request.status());
        if (request.status() == ReclamationStatus.RESOLUE || request.status() == ReclamationStatus.REJETEE) {
            reclamation.setClosedAt(LocalDateTime.now());
        }
        reclamation = reclamationRepository.save(reclamation);
        return mapToResponse(reclamation, getUserOrThrow(reclamation.getUserId()));
    }

    private Reclamation findReclamation(Long id) {
        return reclamationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Réclamation introuvable"));
    }

    private UserSummaryDto getUserOrThrow(Long userId) {
        try {
            return userClient.getUserById(userId);
        } catch (FeignException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Utilisateur introuvable via Gestion-User");
        }
    }

    private ReclamationResponse mapToResponse(Reclamation reclamation, UserSummaryDto user) {
        return new ReclamationResponse(
                reclamation.getId(),
                reclamation.getUserId(),
                user.fullName(),
                user.email(),
                user.role(),
                reclamation.getSubject(),
                reclamation.getCategory(),
                reclamation.getDescription(),
                reclamation.getAttachmentUrl(),
                reclamation.getAttachmentFileName(),
                reclamation.getStatus(),
                reclamation.getPriority(),
                reclamation.getCreatedAt(),
                reclamation.getUpdatedAt(),
                reclamation.getClosedAt(),
                reclamation.getMessages().stream()
                        .map(m -> new ReclamationMessageResponse(
                                m.getId(),
                                m.getSenderId(),
                                m.getSenderName(),
                                m.getSenderRole(),
                                m.getMessage(),
                                m.getCreatedAt()
                        )).toList()
        );
    }

    private StoredAttachment storeAttachment(MultipartFile file) {
        if (file == null || file.isEmpty()) return null;

        String originalName = file.getOriginalFilename() != null
                ? file.getOriginalFilename().replaceAll("[^a-zA-Z0-9._-]", "_")
                : "attachment";
        String extension = originalName.contains(".") ? originalName.substring(originalName.lastIndexOf('.')) : "";
        String storedName = UUID.randomUUID() + extension;

        Path uploadRoot = Paths.get(uploadDir).toAbsolutePath().normalize();
        Path target = uploadRoot.resolve(storedName).normalize();
        if (!target.startsWith(uploadRoot)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nom de fichier invalide");
        }

        try {
            Files.createDirectories(uploadRoot);
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Erreur lors du stockage de la piÃ¨ce jointe");
        }

        String cp = contextPath == null ? "" : contextPath;
        return new StoredAttachment(cp + "/uploads/reclamations/" + storedName, originalName);
    }

    private record StoredAttachment(String url, String originalName) {}
}
