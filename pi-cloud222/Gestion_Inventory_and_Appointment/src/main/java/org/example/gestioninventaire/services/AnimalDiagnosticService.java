package org.example.gestioninventaire.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.gestioninventaire.dtos.request.DiagnosticAssistantChatRequest;
import org.example.gestioninventaire.dtos.request.DiagnosticRequest;
import org.example.gestioninventaire.dtos.response.DiagnosticResponse;
import org.example.gestioninventaire.dtos.response.ImageChatbotResponse;
import org.example.gestioninventaire.entities.Animal;
import org.example.gestioninventaire.exceptions.ResourceNotFoundException;
import org.example.gestioninventaire.repositories.AnimalRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class AnimalDiagnosticService {

    private final AnimalRepository animalRepository;
    private final RestTemplate restTemplate;

    @Value("${ml.service.url:http://localhost:5001}")
    private String mlServiceUrl;

    @Value("${poultry.ml.service.url:http://localhost:5002}")
    private String poultryMlServiceUrl;

    public DiagnosticResponse diagnose(Long requesterId, DiagnosticRequest request) {
        Animal animal = animalRepository.findByIdAndOwnerIdAndIsDeletedFalse(request.getAnimalId(), requesterId)
                .orElseThrow(() -> new ResourceNotFoundException("Animal non trouve pour cet agriculteur"));

        AnalysisBundle analysisBundle = callAnalysisBundle(animal, request);
        List<DiagnosticResponse.DiseasePrediction> predictions = analysisBundle.predictions();
        String localAnalysis = analysisBundle.analysis();

        return DiagnosticResponse.builder()
                .animalReference(animal.getReference())
                .animalEspece(animal.getEspece())
                .predictions(predictions)
                .geminiAnalysis(localAnalysis)
                .disclaimer("Ce diagnostic est indicatif. Consultez un veterinaire pour confirmation.")
                .build();
    }

    public String chat(Long requesterId, DiagnosticRequest request) {
        Animal animal = animalRepository.findByIdAndOwnerIdAndIsDeletedFalse(request.getAnimalId(), requesterId)
                .orElseThrow(() -> new ResourceNotFoundException("Animal non trouve pour cet agriculteur"));

        return callLlamaChat(buildMlPayload(animal, request));
    }

    public String chatIndependent(DiagnosticAssistantChatRequest request) {
        Map<String, Object> body = new HashMap<>();
        body.put("animal_type", request.getAnimalType() != null ? request.getAnimalType().trim() : "");
        body.put("breed", "Unknown");
        body.put("age", null);
        body.put("gender", "Unknown");
        body.put("weight", null);
        body.put("symptom_1", request.getSymptom1() != null ? request.getSymptom1().trim() : "");
        body.put("symptom_2", request.getSymptom2() != null ? request.getSymptom2().trim() : "");
        body.put("symptom_3", request.getSymptom3() != null ? request.getSymptom3().trim() : "");
        body.put("duration", request.getDuration() != null ? request.getDuration().trim() : "");
        body.put("body_temperature", request.getBodyTemperature() != null ? request.getBodyTemperature().trim() : "");
        body.put("question", request.getQuestion() != null ? request.getQuestion().trim() : "");
        body.put("top_n", 3);

        return callLlamaChat(body);
    }

    public ImageChatbotResponse imageChatbot(MultipartFile file, String question, String audience) {
        return callImageChatbot(file, question, audience, mlServiceUrl, "image chatbot");
    }

    public ImageChatbotResponse poultryImageChatbot(MultipartFile file, String question, String audience) {
        return callImageChatbot(file, question, audience, poultryMlServiceUrl, "poultry chatbot");
    }

    private String callLlamaChat(Map<String, Object> body) {
        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    mlServiceUrl + "/llama-chat",
                    new HttpEntity<>(body, jsonHeaders()),
                    Map.class
            );
            if (response.getBody() == null) {
                return "Le chatbot veterinaire local est indisponible pour le moment.";
            }
            Object answer = response.getBody().get("answer");
            return answer instanceof String && !((String) answer).isBlank()
                    ? (String) answer
                    : "Le chatbot veterinaire local est indisponible pour le moment.";
        } catch (Exception e) {
            log.warn("Llama chat failed: {}", e.getMessage());
            return "Le chatbot veterinaire local est indisponible pour le moment.";
        }
    }

    @SuppressWarnings("unchecked")
    private AnalysisBundle callAnalysisBundle(Animal animal, DiagnosticRequest req) {
        try {
            Map<String, Object> payload = buildMlPayload(animal, req);
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    mlServiceUrl + "/analyze",
                    new HttpEntity<>(payload, jsonHeaders()),
                    Map.class
            );

            if (response.getBody() == null) {
                return buildFallbackBundle(animal, req, List.of());
            }

            List<DiagnosticResponse.DiseasePrediction> predictions =
                    mapPredictions((List<Map<String, Object>>) response.getBody().get("predictions"));

            Object analysis = response.getBody().get("analysis");
            String finalAnalysis = analysis instanceof String && !((String) analysis).isBlank()
                    ? (String) analysis
                    : buildFallbackAnalysis(animal, req, predictions);

            return new AnalysisBundle(predictions, finalAnalysis);
        } catch (Exception e) {
            log.warn("Local analysis failed: {}", e.getMessage());
            return buildFallbackBundle(animal, req, List.of());
        }
    }

    @SuppressWarnings("unchecked")
    private List<DiagnosticResponse.DiseasePrediction> callMlService(Animal animal, DiagnosticRequest req) {
        try {
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(buildMlPayload(animal, req), jsonHeaders());
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    mlServiceUrl + "/predict", entity, Map.class
            );

            if (response.getBody() == null) {
                return List.of();
            }

            return mapPredictions((List<Map<String, Object>>) response.getBody().get("predictions"));
        } catch (Exception e) {
            log.error("ML prediction failed for animal {} and request {}: {}", animal.getId(), req, e.getMessage(), e);
            return List.of();
        }
    }

    private List<DiagnosticResponse.DiseasePrediction> mapPredictions(List<Map<String, Object>> preds) {
        if (preds == null || preds.isEmpty()) {
            return List.of();
        }

        return preds.stream()
                .filter(Objects::nonNull)
                .map(p -> DiagnosticResponse.DiseasePrediction.builder()
                        .rank(((Number) p.get("rank")).intValue())
                        .disease((String) p.get("disease"))
                        .probability(((Number) p.get("probability")).doubleValue())
                        .build()
                )
                .collect(Collectors.toList());
    }

    private AnalysisBundle buildFallbackBundle(
            Animal animal,
            DiagnosticRequest req,
            List<DiagnosticResponse.DiseasePrediction> predictions
    ) {
        List<DiagnosticResponse.DiseasePrediction> safePredictions = predictions;
        if (safePredictions == null || safePredictions.isEmpty()) {
            safePredictions = callMlService(animal, req);
        }
        return new AnalysisBundle(safePredictions, buildFallbackAnalysis(animal, req, safePredictions));
    }

    private HttpHeaders jsonHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private Map<String, Object> buildMlPayload(Animal animal, DiagnosticRequest req) {
        Map<String, Object> body = new HashMap<>();
        body.put("animal_type", normalizeAnimalType(animal.getEspece()));
        body.put("breed", "Unknown");
        body.put("age", animal.getAge());
        body.put("gender", "Unknown");
        body.put("weight", animal.getPoids());
        body.put("symptom_1", req.getSymptom1());
        body.put("symptom_2", req.getSymptom2() != null ? req.getSymptom2() : "");
        body.put("symptom_3", req.getSymptom3() != null ? req.getSymptom3() : "");
        body.put("duration", req.getDuration());
        body.put("body_temperature", req.getBodyTemperature());
        body.put("question", req.getQuestion() != null ? req.getQuestion() : "");
        body.put("top_n", 3);
        return body;
    }

    private String buildFallbackAnalysis(
            Animal animal,
            DiagnosticRequest req,
            List<DiagnosticResponse.DiseasePrediction> predictions
    ) {
        String symptoms = List.of(req.getSymptom1(), req.getSymptom2(), req.getSymptom3()).stream()
                .filter(value -> value != null && !value.isBlank())
                .collect(Collectors.joining(", "));

        if (!predictions.isEmpty()) {
            DiagnosticResponse.DiseasePrediction top = predictions.get(0);
            return "Analyse locale simplifiee pour " + animal.getReference() + " (" + animal.getEspece() + ").\n"
                    + "Symptomes observes : " + symptoms + ".\n"
                    + "Hypothese principale : " + top.getDisease() + " (" + top.getProbability() + "%).\n"
                    + "Duree : " + req.getDuration() + ", temperature : " + req.getBodyTemperature() + ".\n"
                    + "Consultez un veterinaire pour confirmer ce diagnostic et adapter la prise en charge.";
        }

        return "Le moteur local n a pas pu produire une analyse detaillee pour le moment. "
                + "Consultez un veterinaire pour un examen clinique.";
    }

    private String normalizeAnimalType(String espece) {
        if (espece == null) {
            return "";
        }

        String normalized = espece.trim().toLowerCase();
        return switch (normalized) {
            case "vache", "cow" -> "cow";
            case "buffle", "buffalo", "buffles" -> "buffalo";
            case "mouton", "sheep" -> "sheep";
            default -> normalized;
        };
    }

    @SuppressWarnings("unchecked")
    private ImageChatbotResponse callImageChatbot(
            MultipartFile file,
            String question,
            String audience,
            String serviceUrl,
            String logContext
    ) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            ByteArrayResource fileResource = new ByteArrayResource(file.getBytes()) {
                @Override
                public String getFilename() {
                    return file.getOriginalFilename() != null ? file.getOriginalFilename() : "uploaded-image.jpg";
                }
            };

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", fileResource);
            body.add("question", question != null ? question : "");
            body.add("audience", audience != null ? audience : "farmer");
            body.add("top_n", "3");

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    serviceUrl + "/predict-image",
                    new HttpEntity<>(body, headers),
                    Map.class
            );

            if (response.getBody() == null) {
                throw new IllegalStateException("Empty response from image chatbot");
            }

            List<Map<String, Object>> rawPredictions =
                    (List<Map<String, Object>>) response.getBody().get("predictions");

            List<ImageChatbotResponse.ImagePrediction> predictions = rawPredictions.stream()
                    .map(item -> ImageChatbotResponse.ImagePrediction.builder()
                            .rank(((Number) item.get("rank")).intValue())
                            .disease((String) item.get("disease"))
                            .probability(((Number) item.get("probability")).doubleValue())
                            .build())
                    .collect(Collectors.toList());

            return ImageChatbotResponse.builder()
                    .predictedLabel((String) response.getBody().get("predicted_label"))
                    .confidence(((Number) response.getBody().get("confidence")).doubleValue())
                    .predictions(predictions)
                    .analysis((String) response.getBody().get("analysis"))
                    .disclaimer((String) response.getBody().get("disclaimer"))
                    .trainingSummary((Map<String, Object>) response.getBody().get("training_summary"))
                    .build();

        } catch (Exception e) {
            log.error("{} failed: {}", logContext, e.getMessage(), e);
            throw new IllegalStateException("Le chatbot image est indisponible pour le moment.");
        }
    }

    private record AnalysisBundle(
            List<DiagnosticResponse.DiseasePrediction> predictions,
            String analysis
    ) {}
}
