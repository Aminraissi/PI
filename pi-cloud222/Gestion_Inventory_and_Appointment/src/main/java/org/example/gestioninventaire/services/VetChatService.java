package org.example.gestioninventaire.services;

import com.google.genai.types.GenerateContentResponse;
import com.google.genai.Client;

import lombok.RequiredArgsConstructor;
import org.example.gestioninventaire.dtos.request.ChatRequest;
import org.example.gestioninventaire.entities.Animal;
import org.example.gestioninventaire.entities.HealthRecord;
import org.example.gestioninventaire.entities.Vaccination;
import org.example.gestioninventaire.exceptions.ResourceNotFoundException;
import org.example.gestioninventaire.repositories.AnimalRepository;
import org.example.gestioninventaire.repositories.HealthRecordRepository;
import org.example.gestioninventaire.repositories.VaccinationRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class VetChatService {

    private final HealthRecordRepository healthRecordRepository;
    private final VaccinationRepository  vaccinationRepository;
    private final AnimalRepository       animalRepository;
    private final RestTemplate           restTemplate;

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    private static final String GEMINI_URL =
            "https://generativelanguage.googleapis.com/v1/models/gemini-3-flash-preview:generateContent?key=";
    public String chat(Long animalId, String question) {

        // 1. Récupérer l'animal
        Animal animal = animalRepository.findById(animalId)
                .orElseThrow(() -> new ResourceNotFoundException("Animal non trouvé"));

        // 2. Récupérer les données médicales depuis la BDD (RAG)
        List<HealthRecord> records = healthRecordRepository.findByAnimalIdWithAnimal(animalId);
        List<Vaccination>  vaccins = vaccinationRepository.findByAnimalId(animalId);

        // 3. Construire le contexte structuré
        String context = buildContext(animal, records, vaccins);

        // 4. Construire le prompt complet
        String fullPrompt = """
                Tu es un assistant vétérinaire expert. Tu réponds UNIQUEMENT en te basant
                sur le dossier médical fourni ci-dessous. Si l'information demandée n'est
                pas dans le dossier, indique-le clairement. Réponds en français, de manière
                concise et professionnelle.

                """ + context + """

                Question du vétérinaire : """ + question;

        // 5. Appel à l'API Gemini
        return callGemini(fullPrompt);
    }

    private String buildContext(Animal animal, List<HealthRecord> records, List<Vaccination> vaccins) {
        StringBuilder sb = new StringBuilder();

        sb.append("=== ANIMAL ===\n");
        sb.append("Espèce       : ").append(animal.getEspece()).append("\n");
        sb.append("Référence    : ").append(animal.getReference()).append("\n");
        sb.append("Poids        : ").append(animal.getPoids()).append(" kg\n");
        sb.append("Naissance    : ").append(animal.getDateNaissance()).append("\n\n");

        sb.append("=== DOSSIERS SANTÉ (").append(records.size()).append(") ===\n");
        if (records.isEmpty()) {
            sb.append("Aucun dossier santé enregistré.\n");
        } else {
            for (HealthRecord r : records) {
                sb.append("- [").append(r.getDateH()).append("] ")
                        .append("Maladie: ").append(r.getMaladie())
                        .append(" | Traitement: ").append(r.getTraitement())
                        .append("\n");
            }
        }

        sb.append("\n=== VACCINATIONS (").append(vaccins.size()).append(") ===\n");
        if (vaccins.isEmpty()) {
            sb.append("Aucune vaccination enregistrée.\n");
        } else {
            for (Vaccination v : vaccins) {
                sb.append("- [").append(v.getDateVaccin()).append("] ")
                        .append("Vaccin: ").append(v.getVaccin())
                        .append(" | Dose: ").append(v.getDose())
                        .append(" | Statut: ").append(v.getStatus())
                        .append("\n");
            }
        }

        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private String callGemini(String prompt) {
        try {
            // Création du client (clé récupérée automatiquement depuis GEMINI_API_KEY)
            Client client = new Client();

            // Appel du modèle Gemini (NOUVEAU modèle valide)
            GenerateContentResponse response =
                    client.models.generateContent(
                            "gemini-2.5-flash", // ✅ modèle actuel supporté
                            prompt,
                            null
                    );

            return response.text();

        } catch (Exception e) {
            throw new RuntimeException("Erreur API Gemini : " + e.getMessage());
        }
    }
}