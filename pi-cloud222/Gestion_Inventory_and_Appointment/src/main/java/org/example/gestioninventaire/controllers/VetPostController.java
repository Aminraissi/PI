package org.example.gestioninventaire.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.gestioninventaire.dtos.request.CreateVetPostRequest;
import org.example.gestioninventaire.dtos.request.UpdateVetPostRequest;
import org.example.gestioninventaire.dtos.response.ApiResponse;
import org.example.gestioninventaire.dtos.response.VetPostResponse;
import org.example.gestioninventaire.services.VetPostService;
import org.example.gestioninventaire.util.JwtUtils;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;


@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class VetPostController {

    private final VetPostService postService;
    private final JwtUtils jwtUtils;


    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<VetPostResponse> create(
            @RequestHeader("Authorization") String authHeader,
            @Valid @RequestPart("data") CreateVetPostRequest req,
            @RequestPart(value = "media", required = false) MultipartFile media) {

        Long vetId = jwtUtils.extractUserId(authHeader);
        return ApiResponse.<VetPostResponse>builder()
                .message("Publication créée avec succès")
                .data(postService.createPost(req, vetId, media))
                .build();
    }


    @GetMapping("/vet/{vetId}")
    public ApiResponse<List<VetPostResponse>> getByVet(@PathVariable Long vetId) {
        return ApiResponse.<List<VetPostResponse>>builder()
                .message("Publications récupérées")
                .data(postService.getPostsByVet(vetId))
                .build();
    }


    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<VetPostResponse> update(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long id,
            @Valid @RequestPart("data") UpdateVetPostRequest req,
            @RequestPart(value = "media", required = false) MultipartFile media) {

        Long vetId = jwtUtils.extractUserId(authHeader);
        return ApiResponse.<VetPostResponse>builder()
                .message("Publication mise à jour")
                .data(postService.updatePost(id, vetId, req, media))
                .build();
    }


    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long id) {

        Long vetId = jwtUtils.extractUserId(authHeader);
        postService.deletePost(id, vetId);
        return ApiResponse.<Void>builder()
                .message("Publication supprimée")
                .build();
    }
}