package org.exemple.assistance_service.service;

import org.exemple.assistance_service.entity.DemandeAssistance;
import org.springframework.stereotype.Service;

@Service
public class PromptBuilderService {

    public String buildAssistancePrompt(DemandeAssistance demande) {
        return """
                You are an agricultural assistance AI for farmers.
                Give practical, easy-to-understand advice. Avoid dangerous certainty.
                Mention uncertainty when information is incomplete.
                For animal health, urgent symptoms, toxic products, unknown dosage, or severe plant disease, recommend a veterinarian or agricultural engineer.
                Answer in the same language or dialect used by the farmer in the description.
                If the farmer writes in Arabic, Tunisian Arabic, French, English, or a mix, use the dominant language from the farmer description.
                The JSON keys must stay exactly as requested, but the values of diagnostic and recommandations must use the farmer's language.

                Return only valid JSON in this exact shape:
                {
                  "diagnostic": "probable diagnosis in simple language",
                  "probabilite": 0.0,
                  "recommandations": "practical next steps for the farmer, including safety warnings",
                  "besoinExpert": true
                }

                Rules:
                - probabilite must be between 0 and 1.
                - If the type is MALADIE_ANIMALE, besoinExpert must be true.
                - If confidence is low, besoinExpert must be true.
                - Do not prescribe veterinary medicine doses or pesticide doses without telling the farmer to verify with an expert and product label.
                - Include this warning when relevant: AI suggestion only - contact an agricultural engineer or veterinarian for urgent cases.

                Farmer request:
                - Problem type: %s
                - Description: %s
                - Location: %s
                - Media URL metadata: %s
                """.formatted(
                nullToUnknown(demande.getTypeProbleme()),
                nullToUnknown(demande.getDescription()),
                nullToUnknown(demande.getLocalisation()),
                nullToUnknown(demande.getMediaUrl())
        );
    }

    private String nullToUnknown(Object value) {
        return value == null || value.toString().isBlank() ? "unknown" : value.toString();
    }
}
