package org.example.gestioninventaire.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.gestioninventaire.dtos.request.CreateConversationRequest;
import org.example.gestioninventaire.dtos.request.SendMessageRequest;
import org.example.gestioninventaire.dtos.response.ApiResponse;
import org.example.gestioninventaire.dtos.response.ChatConversationResponse;
import org.example.gestioninventaire.dtos.response.ChatMessageResponse;
import org.example.gestioninventaire.services.ChatService;
import org.example.gestioninventaire.util.JwtUtils;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final JwtUtils jwtUtils;

    @PostMapping("/conversations")
    public ApiResponse<ChatConversationResponse> createOrGetConversation(
            @RequestHeader("Authorization") String authHeader,
            @Valid @RequestBody CreateConversationRequest request) {
        Long currentUserId = jwtUtils.extractUserId(authHeader);
        return ApiResponse.<ChatConversationResponse>builder()
                .message("Conversation prête")
                .data(chatService.createOrGetConversation(currentUserId, request.getVeterinarianId()))
                .build();
    }

    @GetMapping("/conversations")
    public ApiResponse<List<ChatConversationResponse>> getMyConversations(
            @RequestHeader("Authorization") String authHeader) {
        Long currentUserId = jwtUtils.extractUserId(authHeader);
        return ApiResponse.<List<ChatConversationResponse>>builder()
                .message("Conversations récupérées")
                .data(chatService.getMyConversations(currentUserId))
                .build();
    }

    @GetMapping("/conversations/{conversationId}/messages")
    public ApiResponse<List<ChatMessageResponse>> getMessages(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long conversationId) {
        Long currentUserId = jwtUtils.extractUserId(authHeader);
        return ApiResponse.<List<ChatMessageResponse>>builder()
                .message("Messages récupérés")
                .data(chatService.getMessages(conversationId, currentUserId))
                .build();
    }

    @PostMapping("/conversations/{conversationId}/messages")
    public ApiResponse<ChatMessageResponse> sendMessage(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long conversationId,
            @Valid @RequestBody SendMessageRequest request) {
        Long currentUserId = jwtUtils.extractUserId(authHeader);
        return ApiResponse.<ChatMessageResponse>builder()
                .message("Message envoyé")
                .data(chatService.sendMessage(conversationId, currentUserId, request))
                .build();
    }

    @PostMapping(value = "/conversations/{conversationId}/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<ChatMessageResponse> sendAttachment(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long conversationId,
            @RequestPart("file") MultipartFile file,
            @RequestPart(value = "content", required = false) String content) {
        Long currentUserId = jwtUtils.extractUserId(authHeader);
        return ApiResponse.<ChatMessageResponse>builder()
                .message("Fichier envoyé")
                .data(chatService.sendAttachmentMessage(conversationId, currentUserId, file, content))
                .build();
    }
}
