package org.example.gestioninventaire.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.gestioninventaire.dtos.request.ChatRequest;
import org.example.gestioninventaire.dtos.response.ApiResponse;
import org.example.gestioninventaire.services.VetChatService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class VetChatController {

    private final VetChatService vetChatService;

    @PostMapping
    public ApiResponse<String> chat(
            @RequestHeader("Authorization") String authHeader,
            @Valid @RequestBody ChatRequest request
    ) {
        String response = vetChatService.chat(request.getAnimalId(), request.getQuestion());
        return ApiResponse.<String>builder()
                .message("Réponse générée avec succès")
                .data(response)
                .build();
    }
}