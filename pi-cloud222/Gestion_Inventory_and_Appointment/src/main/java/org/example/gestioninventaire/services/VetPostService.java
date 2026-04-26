package org.example.gestioninventaire.services;

import lombok.RequiredArgsConstructor;
import org.example.gestioninventaire.dtos.request.CreateVetPostRequest;
import org.example.gestioninventaire.dtos.request.UpdateVetPostRequest;
import org.example.gestioninventaire.dtos.response.VetPostResponse;
import org.example.gestioninventaire.entities.VetPost;
import org.example.gestioninventaire.exceptions.BadRequestException;
import org.example.gestioninventaire.repositories.VetPostRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VetPostService {

    private final VetPostRepository postRepository;

    @Value("${app.posts.upload-dir:post-uploads}")
    private String uploadDir;

    @Value("${server.servlet.context-path:}")
    private String contextPath;

    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp", "image/gif"
    );
    private static final Set<String> ALLOWED_VIDEO_TYPES = Set.of(
            "video/mp4", "video/webm", "video/ogg", "video/quicktime"
    );



    public VetPostResponse createPost(CreateVetPostRequest req, Long vetId, MultipartFile media) {
        String mediaUrl = null;
        String mediaFileName = null;

        if (media != null && !media.isEmpty()) {
            StoredMedia stored = storeMedia(media, req.getType().name());
            mediaUrl = stored.url();
            mediaFileName = stored.originalName();
        }

        VetPost post = VetPost.builder()
                .veterinarianId(vetId)
                .titre(req.getTitre())
                .description(req.getDescription())
                .type(req.getType())
                .mediaUrl(mediaUrl)
                .mediaFileName(mediaFileName)
                .build();

        return toResponse(postRepository.save(post));
    }

    public List<VetPostResponse> getPostsByVet(Long vetId) {
        return postRepository.findByVeterinarianIdOrderByCreatedAtDesc(vetId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public VetPostResponse updatePost(Long postId, Long vetId, UpdateVetPostRequest req, MultipartFile media) {
        VetPost post = postRepository.findById(postId)
                .orElseThrow(() -> new BadRequestException("Publication introuvable"));
        if (!post.getVeterinarianId().equals(vetId)) {
            throw new BadRequestException("Vous n'êtes pas autorisé à modifier cette publication");
        }

        post.setTitre(req.getTitre());
        post.setDescription(req.getDescription());
        post.setType(req.getType());

        if (media != null && !media.isEmpty()) {
            // Delete old file if exists
            deleteFile(post.getMediaUrl());
            StoredMedia stored = storeMedia(media, req.getType().name());
            post.setMediaUrl(stored.url());
            post.setMediaFileName(stored.originalName());
        }

        return toResponse(postRepository.save(post));
    }

    public void deletePost(Long postId, Long vetId) {
        VetPost post = postRepository.findById(postId)
                .orElseThrow(() -> new BadRequestException("Publication introuvable"));
        if (!post.getVeterinarianId().equals(vetId)) {
            throw new BadRequestException("Vous n'êtes pas autorisé à supprimer cette publication");
        }
        deleteFile(post.getMediaUrl());
        postRepository.delete(post);
    }

    //File storage

    private StoredMedia storeMedia(MultipartFile file, String postType) {
        String mimeType = file.getContentType() != null ? file.getContentType().toLowerCase() : "";
        Set<String> allowed = "VIDEO".equals(postType) ? ALLOWED_VIDEO_TYPES : ALLOWED_IMAGE_TYPES;

        if (!allowed.contains(mimeType)) {
            throw new BadRequestException("Type de fichier non autorisé pour ce type de publication");
        }

        String originalName = file.getOriginalFilename() != null
                ? file.getOriginalFilename().replaceAll("[^a-zA-Z0-9._-]", "_")
                : "file";
        String ext = originalName.contains(".") ? originalName.substring(originalName.lastIndexOf('.')) : "";
        String storedName = UUID.randomUUID() + ext;

        Path uploadRoot = Paths.get(uploadDir).toAbsolutePath().normalize();
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
            throw new BadRequestException("Erreur lors du stockage du fichier : " + e.getMessage());
        }

        String cp = contextPath == null ? "" : contextPath;
        String url = cp + "/post-uploads/" + storedName;
        return new StoredMedia(url, originalName);
    }

    private void deleteFile(String mediaUrl) {
        if (mediaUrl == null || mediaUrl.isBlank()) return;
        try {
            String fileName = mediaUrl.substring(mediaUrl.lastIndexOf('/') + 1);
            Path file = Paths.get(uploadDir).toAbsolutePath().normalize().resolve(fileName).normalize();
            Files.deleteIfExists(file);
        } catch (IOException ignored) {
            // Non-critical
        }
    }

    private VetPostResponse toResponse(VetPost p) {
        return VetPostResponse.builder()
                .id(p.getId())
                .veterinarianId(p.getVeterinarianId())
                .titre(p.getTitre())
                .description(p.getDescription())
                .type(p.getType())
                .mediaUrl(p.getMediaUrl())
                .mediaFileName(p.getMediaFileName())
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .build();
    }

    private record StoredMedia(String url, String originalName) {}
}