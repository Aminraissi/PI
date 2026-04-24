package org.example.gestioninventaire.dtos.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ChatMessageResponse {
    private Long id;
    private Long conversationId;
    private Long senderId;
    private Long receiverId;
    private String content;
    private String messageType;
    private String attachmentUrl;
    private String attachmentFileName;
    private String attachmentMimeType;
    private Long attachmentSize;
    private LocalDateTime sentAt;
}
