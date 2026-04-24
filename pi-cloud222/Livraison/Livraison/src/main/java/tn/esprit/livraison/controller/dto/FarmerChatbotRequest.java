package tn.esprit.livraison.controller.dto;

public record FarmerChatbotRequest(
        Integer farmerId,
        String message
) {
}
