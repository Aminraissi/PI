package org.example.gestioninventaire.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.gestioninventaire.dtos.request.DiagnosticAssistantChatRequest;
import org.example.gestioninventaire.dtos.request.DiagnosticRequest;
import org.example.gestioninventaire.dtos.response.ApiResponse;
import org.example.gestioninventaire.dtos.response.DiagnosticChatResponse;
import org.example.gestioninventaire.dtos.response.DiagnosticResponse;
import org.example.gestioninventaire.dtos.response.ImageChatbotResponse;
import org.example.gestioninventaire.services.AnimalDiagnosticService;
import org.example.gestioninventaire.util.JwtUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/diagnostic")
@RequiredArgsConstructor
public class AnimalDiagnosticController {

    private final AnimalDiagnosticService diagnosticService;
    private final JwtUtils jwtUtils;

    @PostMapping
    public ApiResponse<DiagnosticResponse> diagnose(
            @RequestHeader("Authorization") String authHeader,
            @Valid @RequestBody DiagnosticRequest request
    ) {
        Long requesterId = jwtUtils.extractUserId(authHeader);
        DiagnosticResponse result = diagnosticService.diagnose(requesterId, request);
        return ApiResponse.<DiagnosticResponse>builder()
                .message("Diagnostic genere avec succes")
                .data(result)
                .build();
    }

    @PostMapping("/chat")
    public ApiResponse<DiagnosticChatResponse> chat(
            @RequestHeader("Authorization") String authHeader,
            @Valid @RequestBody DiagnosticRequest request
    ) {
        Long requesterId = jwtUtils.extractUserId(authHeader);
        String answer = diagnosticService.chat(requesterId, request);
        return ApiResponse.<DiagnosticChatResponse>builder()
                .message("Reponse de chat generee avec succes")
                .data(DiagnosticChatResponse.builder().answer(answer).build())
                .build();
    }

    @PostMapping("/assistant/chat")
    public ApiResponse<DiagnosticChatResponse> assistantChat(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody DiagnosticAssistantChatRequest request
    ) {
        jwtUtils.extractUserId(authHeader);
        String answer = diagnosticService.chatIndependent(request);
        return ApiResponse.<DiagnosticChatResponse>builder()
                .message("Reponse du chatbot generee avec succes")
                .data(DiagnosticChatResponse.builder().answer(answer).build())
                .build();
    }

    @PostMapping(value = "/image-chatbot", consumes = "multipart/form-data")
    public ApiResponse<ImageChatbotResponse> imageChatbot(
            @RequestHeader("Authorization") String authHeader,
            @RequestPart("file") MultipartFile file,
            @RequestPart(value = "question", required = false) String question,
            @RequestPart(value = "audience", required = false) String audience
    ) {
        jwtUtils.extractUserId(authHeader);
        ImageChatbotResponse response = diagnosticService.imageChatbot(file, question, audience);
        return ApiResponse.<ImageChatbotResponse>builder()
                .message("Analyse image chatbot generee avec succes")
                .data(response)
                .build();
    }

    @PostMapping(value = "/poultry-image-chatbot", consumes = "multipart/form-data")
    public ApiResponse<ImageChatbotResponse> poultryImageChatbot(
            @RequestHeader("Authorization") String authHeader,
            @RequestPart("file") MultipartFile file,
            @RequestPart(value = "question", required = false) String question,
            @RequestPart(value = "audience", required = false) String audience
    ) {
        jwtUtils.extractUserId(authHeader);
        ImageChatbotResponse response = diagnosticService.poultryImageChatbot(file, question, audience);
        return ApiResponse.<ImageChatbotResponse>builder()
                .message("Analyse image poultry generee avec succes")
                .data(response)
                .build();
    }
}
