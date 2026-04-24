package tn.esprit.livraison.controller.dto;

import java.time.LocalDateTime;

public record FarmerChatbotResponse(
        String reply,
        String model,
        LocalDateTime createdAt
) {
}
