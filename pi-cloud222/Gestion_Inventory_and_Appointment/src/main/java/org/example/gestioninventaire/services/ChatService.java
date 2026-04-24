package org.example.gestioninventaire.services;

import lombok.RequiredArgsConstructor;
import org.example.gestioninventaire.config.ChatWebSocketHandler;
import org.example.gestioninventaire.dtos.request.SendMessageRequest;
import org.example.gestioninventaire.dtos.response.ChatConversationResponse;
import org.example.gestioninventaire.dtos.response.ChatMessageResponse;
import org.example.gestioninventaire.dtos.response.UserResponse;
import org.example.gestioninventaire.dtos.response.UserSummaryResponse;
import org.example.gestioninventaire.entities.ChatConversation;
import org.example.gestioninventaire.entities.ChatMessage;
import org.example.gestioninventaire.exceptions.BadRequestException;
import org.example.gestioninventaire.exceptions.ResourceNotFoundException;
import org.example.gestioninventaire.feigns.UserClient;
import org.example.gestioninventaire.repositories.ChatConversationRepository;
import org.example.gestioninventaire.repositories.ChatMessageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatConversationRepository conversationRepository;
    private final ChatMessageRepository messageRepository;
    private final UserClient userClient;
    private final ChatWebSocketHandler chatWebSocketHandler;
    private final ChatAttachmentStorageService chatAttachmentStorageService;

    @Transactional
    public ChatConversationResponse createOrGetConversation(Long farmerId, Long veterinarianId) {
        UserResponse vet = userClient.getUserById(veterinarianId);
        if (vet == null || vet.getId() == null || !"VETERINAIRE".equalsIgnoreCase(vet.getRole())) {
            throw new BadRequestException("Le destinataire choisi n'est pas un vétérinaire");
        }

        ChatConversation conversation = conversationRepository
                .findByFarmerIdAndVeterinarianId(farmerId, veterinarianId)
                .orElseGet(() -> conversationRepository.save(ChatConversation.builder()
                        .farmerId(farmerId)
                        .veterinarianId(veterinarianId)
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build()));

        return toConversationResponse(conversation, farmerId);
    }

    @Transactional(readOnly = true)
    public List<ChatConversationResponse> getMyConversations(Long currentUserId) {
        return conversationRepository.findByFarmerIdOrVeterinarianIdOrderByUpdatedAtDesc(currentUserId, currentUserId)
                .stream()
                .map(conversation -> toConversationResponse(conversation, currentUserId))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ChatMessageResponse> getMessages(Long conversationId, Long currentUserId) {
        ChatConversation conversation = getAuthorizedConversation(conversationId, currentUserId);
        return messageRepository.findByConversationIdOrderBySentAtAsc(conversation.getId())
                .stream()
                .map(this::toMessageResponse)
                .toList();
    }

    @Transactional
    public ChatMessageResponse sendMessage(Long conversationId, Long currentUserId, SendMessageRequest request) {
        ChatConversation conversation = getAuthorizedConversation(conversationId, currentUserId);
        Long receiverId = currentUserId.equals(conversation.getFarmerId())
                ? conversation.getVeterinarianId()
                : conversation.getFarmerId();

        ChatMessage message = messageRepository.save(ChatMessage.builder()
                .conversation(conversation)
                .senderId(currentUserId)
                .receiverId(receiverId)
                .content(request.getContent().trim())
                .messageType("TEXT")
                .sentAt(LocalDateTime.now())
                .build());

        conversation.setUpdatedAt(message.getSentAt());
        conversationRepository.save(conversation);

        ChatMessageResponse payload = toMessageResponse(message);
        chatWebSocketHandler.sendToUser(receiverId, payload);
        chatWebSocketHandler.sendToUser(currentUserId, payload);
        return payload;
    }

    @Transactional
    public ChatMessageResponse sendAttachmentMessage(Long conversationId, Long currentUserId, MultipartFile file, String content) {
        ChatConversation conversation = getAuthorizedConversation(conversationId, currentUserId);
        Long receiverId = currentUserId.equals(conversation.getFarmerId())
                ? conversation.getVeterinarianId()
                : conversation.getFarmerId();

        ChatAttachmentStorageService.StoredAttachment stored = chatAttachmentStorageService.store(file);
        String normalizedContent = content == null ? "" : content.trim();

        ChatMessage message = messageRepository.save(ChatMessage.builder()
                .conversation(conversation)
                .senderId(currentUserId)
                .receiverId(receiverId)
                .content(normalizedContent)
                .messageType(stored.messageType())
                .attachmentUrl(stored.url())
                .attachmentFileName(stored.fileName())
                .attachmentMimeType(stored.mimeType())
                .attachmentSize(stored.size())
                .sentAt(LocalDateTime.now())
                .build());

        conversation.setUpdatedAt(message.getSentAt());
        conversationRepository.save(conversation);

        ChatMessageResponse payload = toMessageResponse(message);
        chatWebSocketHandler.sendToUser(receiverId, payload);
        chatWebSocketHandler.sendToUser(currentUserId, payload);
        return payload;
    }

    private ChatConversation getAuthorizedConversation(Long conversationId, Long currentUserId) {
        ChatConversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation introuvable"));
        if (!currentUserId.equals(conversation.getFarmerId()) && !currentUserId.equals(conversation.getVeterinarianId())) {
            throw new BadRequestException("Vous n'avez pas accès à cette conversation");
        }
        return conversation;
    }

    private ChatConversationResponse toConversationResponse(ChatConversation conversation, Long currentUserId) {
        UserSummaryResponse farmer = toSummary(userClient.getUserById(conversation.getFarmerId()));
        UserSummaryResponse vet = toSummary(userClient.getUserById(conversation.getVeterinarianId()));
        ChatMessage lastMessage = messageRepository.findTopByConversationIdOrderBySentAtDesc(conversation.getId());
        long unreadCount = messageRepository.countByConversationIdAndReceiverId(conversation.getId(), currentUserId);

        UserSummaryResponse other = currentUserId.equals(conversation.getFarmerId()) ? vet : farmer;

        return ChatConversationResponse.builder()
                .id(conversation.getId())
                .farmer(farmer)
                .veterinarian(vet)
                .otherParticipant(other)
                .lastMessage(lastMessage != null ? buildPreview(lastMessage) : null)
                .lastMessageAt(lastMessage != null ? lastMessage.getSentAt() : conversation.getUpdatedAt())
                .unreadCount(unreadCount)
                .build();
    }

    private UserSummaryResponse toSummary(UserResponse user) {
        return UserSummaryResponse.builder()
                .id(user.getId())
                .nom(user.getNom())
                .prenom(user.getPrenom())
                .email(user.getEmail())
                .telephone(user.getTelephone())
                .role(user.getRole())
                .build();
    }

    private ChatMessageResponse toMessageResponse(ChatMessage message) {
        return ChatMessageResponse.builder()
                .id(message.getId())
                .conversationId(message.getConversation().getId())
                .senderId(message.getSenderId())
                .receiverId(message.getReceiverId())
                .content(message.getContent())
                .messageType(message.getMessageType() == null ? "TEXT" : message.getMessageType())
                .attachmentUrl(message.getAttachmentUrl())
                .attachmentFileName(message.getAttachmentFileName())
                .attachmentMimeType(message.getAttachmentMimeType())
                .attachmentSize(message.getAttachmentSize())
                .sentAt(message.getSentAt())
                .build();
    }

    private String buildPreview(ChatMessage message) {
        if (message.getContent() != null && !message.getContent().isBlank()) {
            return message.getContent();
        }
        String type = message.getMessageType() == null ? "TEXT" : message.getMessageType();
        return switch (type) {
            case "IMAGE" -> "Image envoyée";
            case "AUDIO" -> "Message vocal";
            case "FILE" -> "Pièce jointe";
            default -> "Message";
        };
    }
}
