package org.example.gestioninventaire.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.gestioninventaire.dtos.response.MedicalAssistantAnswerResponse;
import org.example.gestioninventaire.entities.Animal;
import org.example.gestioninventaire.entities.HealthRecord;
import org.example.gestioninventaire.enums.AppointmentStatus;
import org.example.gestioninventaire.exceptions.ResourceNotFoundException;
import org.example.gestioninventaire.repositories.AnimalRepository;
import org.example.gestioninventaire.repositories.AppointmentRepository;
import org.example.gestioninventaire.repositories.HealthRecordRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class VetMedicalAssistantService {

    private static final String GROQ_CHAT_URL = "https://api.groq.com/openai/v1/chat/completions";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final Set<String> STOP_WORDS = Set.of(
            "le", "la", "les", "de", "des", "du", "un", "une", "et", "ou", "en", "au", "aux",
            "a", "est", "il", "elle", "sur", "pour", "par", "avec", "dans", "son", "sa", "ses",
            "quel", "quelle", "quels", "quelles", "donne", "faire", "fais", "moi", "que", "qui",
            "quoi", "dernier", "derniere", "resume", "dossier", "medical", "animal"
    );

    private final AnimalRepository animalRepository;
    private final HealthRecordRepository healthRecordRepository;
    private final AppointmentRepository appointmentRepository;
    private final RestTemplate restTemplate;

    @Value("${groq.enabled:true}")
    private boolean groqEnabled;

    @Value("${groq.api.key:}")
    private String groqApiKey;

    @Value("${groq.model:llama-3.3-70b-versatile}")
    private String groqModel;

    public MedicalAssistantAnswerResponse askQuestion(Long veterinarianId, Long animalId, String question) {
        Animal animal = animalRepository.findById(animalId)
                .orElseThrow(() -> new ResourceNotFoundException("Animal non trouve"));

        boolean allowed = appointmentRepository.existsByVeterinarianIdAndAnimal_IdAndAppointmentStatus(
                veterinarianId,
                animalId,
                AppointmentStatus.ACCEPTEE
        );
        if (!allowed) {
            throw new ResourceNotFoundException("Vous n'avez pas acces au dossier medical de cet animal");
        }

        List<HealthRecord> records = healthRecordRepository.findByAnimalIdWithAnimal(animalId)
                .stream()
                .sorted(Comparator.comparing(HealthRecord::getDateH, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .toList();

        List<String> retrievedContext = retrieveRelevantContext(animal, records, question);
        String summary = buildMedicalSummary(animal, records);
        String lastDisease = records.isEmpty() ? "Aucune maladie enregistree" : safe(records.get(0).getMaladie());
        String answer = callGroq(question, animal, records, summary, retrievedContext);

        if (answer == null || answer.isBlank()) {
            answer = buildFallbackAnswer(question, summary, lastDisease, records, retrievedContext);
        }

        return MedicalAssistantAnswerResponse.builder()
                .answer(answer)
                .aiProvider(isGroqConfigured() ? "groq" : "local")
                .aiModel(isGroqConfigured() ? groqModel : "local-rag-summary")
                .medicalSummary(summary)
                .lastDisease(lastDisease)
                .recordCount(records.size())
                .usedContext(retrievedContext)
                .build();
    }

    private boolean isGroqConfigured() {
        return groqEnabled && groqApiKey != null && !groqApiKey.isBlank();
    }

    private String callGroq(
            String question,
            Animal animal,
            List<HealthRecord> records,
            String summary,
            List<String> retrievedContext
    ) {
        if (!isGroqConfigured()) {
            return null;
        }

        StringBuilder prompt = new StringBuilder();
        prompt.append("Tu es un assistant RAG pour un veterinaire. ");
        prompt.append("Tu dois repondre uniquement a partir du contexte fourni. ");
        prompt.append("Si une information manque dans le dossier, dis clairement qu'elle n'est pas disponible. ");
        prompt.append("Reponds en francais, de facon concise, fiable et exploitable.\n\n");
        prompt.append("=== ANIMAL ===\n");
        prompt.append("Reference: ").append(safe(animal.getReference())).append("\n");
        prompt.append("Espece: ").append(safe(animal.getEspece())).append("\n");
        prompt.append("Poids: ").append(animal.getPoids() != null ? animal.getPoids() + " kg" : "inconnu").append("\n");
        prompt.append("Date de naissance: ").append(animal.getDateNaissance() != null ? animal.getDateNaissance() : "inconnue").append("\n\n");
        prompt.append("=== RESUME DU DOSSIER ===\n").append(summary).append("\n\n");
        prompt.append("=== EXTRAITS RETROUVES ===\n");
        for (String chunk : retrievedContext) {
            prompt.append("- ").append(chunk).append("\n");
        }
        if (retrievedContext.isEmpty()) {
            prompt.append("- Aucun extrait specifique retrouve, base-toi sur le resume general.\n");
        }
        prompt.append("\n=== NOMBRE D'ENTREES MEDICALES ===\n").append(records.size()).append("\n\n");
        prompt.append("Question du veterinaire: ").append(question);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(groqApiKey);

        Map<String, Object> body = Map.of(
                "model", groqModel,
                "temperature", 0.2,
                "max_tokens", 700,
                "messages", List.of(
                        Map.of("role", "system", "content", "Tu aides un veterinaire a interroger un dossier medical animal via un contexte RAG."),
                        Map.of("role", "user", "content", prompt.toString())
                )
        );

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    GROQ_CHAT_URL,
                    new HttpEntity<>(body, headers),
                    Map.class
            );
            return extractGroqText(response);
        } catch (Exception e) {
            log.warn("Groq medical assistant unavailable for animal {}: {}", animal.getId(), e.getMessage());
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private String extractGroqText(ResponseEntity<Map> response) {
        if (response.getBody() == null) {
            return null;
        }
        List<Map<String, Object>> choices = (List<Map<String, Object>>) response.getBody().get("choices");
        if (choices == null || choices.isEmpty()) {
            return null;
        }
        Object messageObj = choices.get(0).get("message");
        if (!(messageObj instanceof Map<?, ?> message)) {
            return null;
        }
        Object content = message.get("content");
        return content instanceof String text ? text.trim() : null;
    }

    private List<String> retrieveRelevantContext(Animal animal, List<HealthRecord> records, String question) {
        List<String> chunks = new ArrayList<>();
        chunks.add("Animal " + safe(animal.getReference()) + " de type " + safe(animal.getEspece()) +
                ", poids " + (animal.getPoids() != null ? animal.getPoids() + " kg" : "inconnu") +
                ", naissance " + (animal.getDateNaissance() != null ? animal.getDateNaissance() : "inconnue") + ".");

        for (HealthRecord record : records) {
            chunks.add(formatRecordChunk(record));
        }

        Set<String> tokens = tokenize(question);
        return chunks.stream()
                .sorted(Comparator
                        .comparingInt((String chunk) -> scoreChunk(chunk, tokens)).reversed()
                        .thenComparingInt(String::length))
                .limit(5)
                .toList();
    }

    private int scoreChunk(String chunk, Set<String> tokens) {
        String normalizedChunk = normalize(chunk);
        int score = 0;
        for (String token : tokens) {
            if (normalizedChunk.contains(token)) {
                score += 3;
            }
        }
        if (normalizedChunk.contains("date")) {
            score += 1;
        }
        if (normalizedChunk.contains("maladie")) {
            score += 1;
        }
        return score;
    }

    private Set<String> tokenize(String text) {
        return Arrays.stream(normalize(text).split("\\s+"))
                .map(String::trim)
                .filter(token -> token.length() > 2)
                .filter(token -> !STOP_WORDS.contains(token))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private String normalize(String text) {
        return safe(text)
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String buildMedicalSummary(Animal animal, List<HealthRecord> records) {
        if (records.isEmpty()) {
            return "Aucun antecedent medical enregistre pour " + safe(animal.getReference()) + ".";
        }

        HealthRecord latest = records.get(0);
        String diseases = records.stream()
                .map(HealthRecord::getMaladie)
                .filter(value -> value != null && !value.isBlank())
                .distinct()
                .collect(Collectors.joining(", "));

        return "L'animal " + safe(animal.getReference()) + " (" + safe(animal.getEspece()) + ") a "
                + records.size() + " dossier(s) medical(aux). Derniere maladie enregistree: "
                + safe(latest.getMaladie()) + " le " + formatDate(latest) + ". "
                + "Traitement le plus recent: " + safe(latest.getTraitement()) + ". "
                + "Maladies historisees: " + (diseases.isBlank() ? "aucune precisee" : diseases) + ".";
    }

    private String buildFallbackAnswer(
            String question,
            String summary,
            String lastDisease,
            List<HealthRecord> records,
            List<String> retrievedContext
    ) {
        String normalizedQuestion = normalize(question);
        if (normalizedQuestion.contains("resume")) {
            return summary;
        }
        if (normalizedQuestion.contains("derni") && normalizedQuestion.contains("malad")) {
            if (records.isEmpty()) {
                return "Aucune maladie n'est enregistree dans le dossier medical de cet animal.";
            }
            HealthRecord latest = records.get(0);
            return "La derniere maladie enregistree est " + lastDisease + " le " + formatDate(latest)
                    + ". Traitement associe: " + safe(latest.getTraitement()) + ".";
        }

        StringBuilder answer = new StringBuilder(summary);
        if (!retrievedContext.isEmpty()) {
            answer.append("\n\nExtraits utiles:\n");
            retrievedContext.forEach(chunk -> answer.append("- ").append(chunk).append("\n"));
        }
        answer.append("\nReponse generee sans Groq: configurez groq.api.key pour une reponse conversationnelle plus riche.");
        return answer.toString().trim();
    }

    private String formatRecordChunk(HealthRecord record) {
        return "Date: " + formatDate(record)
                + " | Maladie: " + safe(record.getMaladie())
                + " | Traitement: " + safe(record.getTraitement());
    }

    private String formatDate(HealthRecord record) {
        return record.getDateH() != null ? record.getDateH().format(DATE_FORMATTER) : "date inconnue";
    }

    private String safe(Object value) {
        return value == null ? "inconnu" : String.valueOf(value);
    }
}