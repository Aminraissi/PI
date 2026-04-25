package tn.esprit.forums.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URLConnection;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Service
public class ForumImageModerationService {

    public enum ModerationStatus {
        APPROVED,
        REJECTED,
        REVIEW_REQUIRED
    }

    public record ModerationOutcome(ModerationStatus status, String reason) {
    }

    private static final Logger LOG = LoggerFactory.getLogger(ForumImageModerationService.class);
    private static final String REJECTED_MESSAGE =
            "This image violates community guidelines and cannot be published.";
    private static final String REVIEW_REASON_DISABLED = "Image moderation is disabled, so admin review is required.";
    private static final String REVIEW_REASON_MISSING_KEY = "HF_TOKEN is missing, so admin review is required.";
    private static final String REVIEW_REASON_UNSUPPORTED_MEDIA = "Unsupported media reference; admin review is required.";
    private static final String REVIEW_REASON_EMPTY_RESPONSE = "Hugging Face returned an empty moderation response; admin review is required.";
    private static final String REVIEW_REASON_MISSING_RESULTS = "Hugging Face moderation response was incomplete; admin review is required.";
    private static final String REVIEW_REASON_HTTP_ERROR = "Hugging Face moderation request failed; admin review is required.";
    private static final String REVIEW_REASON_CLIENT_ERROR = "Hugging Face moderation could not process this image; admin review is required.";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${forums.ai.image-moderation.enabled:true}")
    private boolean imageModerationEnabled;

    @Value("${forums.ai.image-moderation.provider:huggingface}")
    private String moderationProvider;

    @Value("${forums.ai.huggingface.api-key:}")
    private String huggingFaceApiKey;

    @Value("${forums.ai.huggingface.model:Falconsai/nsfw_image_detection}")
    private String huggingFaceModel;

    @Value("${forums.ai.huggingface.endpoint:https://router.huggingface.co/hf-inference/models/Falconsai/nsfw_image_detection}")
    private String huggingFaceEndpoint;

    @Value("${forums.ai.huggingface.context-model:Ateeqq/nsfw-image-detection}")
    private String huggingFaceContextModel;

    @Value("${forums.ai.huggingface.context-endpoint:https://router.huggingface.co/hf-inference/models/Ateeqq/nsfw-image-detection}")
    private String huggingFaceContextEndpoint;

    @Value("${forums.ai.huggingface.threshold.reject:0.70}")
    private double rejectThreshold;

    @Value("${forums.ai.huggingface.threshold.review:0.40}")
    private double reviewThreshold;

    public ForumImageModerationService(
            @Qualifier("externalRestTemplate") RestTemplate restTemplate,
            ObjectMapper objectMapper
    ) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    public ModerationOutcome moderatePostMedia(List<String> mediaUrls) {
        if (mediaUrls == null || mediaUrls.isEmpty()) {
            return new ModerationOutcome(ModerationStatus.APPROVED, "");
        }

        if (!imageModerationEnabled) {
            LOG.warn("Image moderation fallback: {}", REVIEW_REASON_DISABLED);
            return new ModerationOutcome(ModerationStatus.REVIEW_REQUIRED, REVIEW_REASON_DISABLED);
        }

        if (huggingFaceApiKey == null || huggingFaceApiKey.isBlank()) {
            LOG.warn("Image moderation skipped because HF_TOKEN is missing");
            return new ModerationOutcome(ModerationStatus.REVIEW_REQUIRED, REVIEW_REASON_MISSING_KEY);
        }

        for (String mediaUrl : mediaUrls) {
            if (!looksLikeImage(mediaUrl)) {
                LOG.warn("Image moderation requires manual review for unsupported media reference");
                return new ModerationOutcome(ModerationStatus.REVIEW_REQUIRED, REVIEW_REASON_UNSUPPORTED_MEDIA);
            }

            ModerationOutcome outcome = moderateSingleImage(mediaUrl);
            if (outcome.status() != ModerationStatus.APPROVED) {
                return outcome;
            }
        }

        return new ModerationOutcome(ModerationStatus.APPROVED, "");
    }

    private ModerationOutcome moderateSingleImage(String mediaUrl) {
        try {
            ImagePayload imagePayload = loadImagePayload(mediaUrl);
            if (imagePayload == null || imagePayload.bytes().length == 0) {
                LOG.warn("Image moderation fallback: could not resolve image bytes for moderation");
                return new ModerationOutcome(ModerationStatus.REVIEW_REQUIRED, REVIEW_REASON_UNSUPPORTED_MEDIA);
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(imagePayload.contentType()));
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));
            headers.setBearerAuth(huggingFaceApiKey);

            HttpEntity<byte[]> entity = new HttpEntity<>(imagePayload.bytes(), headers);
            ResponseEntity<String> response = restTemplate.postForEntity(huggingFaceEndpoint, entity, String.class);
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null || response.getBody().isBlank()) {
                LOG.warn("Hugging Face image moderation returned status {} with empty body={}", response.getStatusCode(), response.getBody() == null);
                return new ModerationOutcome(ModerationStatus.REVIEW_REQUIRED, REVIEW_REASON_EMPTY_RESPONSE);
            }

            JsonNode root = objectMapper.readTree(response.getBody());
            if (!root.isArray() || root.isEmpty()) {
                LOG.warn("Hugging Face image moderation response missing results: {}", response.getBody());
                return new ModerationOutcome(ModerationStatus.REVIEW_REQUIRED, REVIEW_REASON_MISSING_RESULTS);
            }

            double nsfwScore = extractLabelScore(root, "nsfw");
            double normalScore = extractLabelScore(root, "normal");
            ModelScores contextScores = queryModelScores(imagePayload, huggingFaceContextEndpoint);
            double sensitiveScore = maxScore(
                    contextScores.score("nsfw"),
                    contextScores.score("nude"),
                    contextScores.score("nudity"),
                    contextScores.score("porn"),
                    contextScores.score("pornography"),
                    contextScores.score("sexual"),
                    contextScores.score("explicit"),
                    contextScores.score("gore"),
                    contextScores.score("blood"),
                    contextScores.score("violent"),
                    contextScores.score("violence"),
                    contextScores.score("graphic"),
                    contextScores.score("graphic violence"),
                    contextScores.score("graphic_violence"),
                    contextScores.score("gore_bloodshed"),
                    contextScores.score("gore_bloodshed_violent")
            );
            double safeContextScore = maxScore(
                    contextScores.score("safe"),
                    contextScores.score("sfw"),
                    contextScores.score("normal"),
                    contextScores.score("non-graphic"),
                    contextScores.score("non-violent"),
                    contextScores.score("non_violent")
            );

            LOG.info(
                    "Hugging Face moderation scores nsfwModel={} nsfw={} normal={} contextModel={} sensitive={} safeContext={}",
                    huggingFaceModel,
                    nsfwScore,
                    normalScore,
                    huggingFaceContextModel,
                    sensitiveScore,
                    safeContextScore
            );

            if (nsfwScore >= rejectThreshold || sensitiveScore >= rejectThreshold) {
                LOG.info("Hugging Face rejected forum image with nsfw score {} and context-sensitive score {}", nsfwScore, sensitiveScore);
                return new ModerationOutcome(ModerationStatus.REJECTED, REJECTED_MESSAGE);
            }

            if (nsfwScore >= reviewThreshold || sensitiveScore >= reviewThreshold) {
                String reason = String.format(
                        Locale.ROOT,
                        "Automatic image moderation was inconclusive (nsfw=%.3f, sensitive=%.3f), so admin review is required.",
                        nsfwScore,
                        sensitiveScore
                );
                LOG.warn("Image moderation fallback: {}", reason);
                return new ModerationOutcome(ModerationStatus.REVIEW_REQUIRED, reason);
            }

            if (normalScore > 0 || safeContextScore > 0 || nsfwScore >= 0 || sensitiveScore >= 0) {
                LOG.info("Hugging Face approved forum image moderation request");
                return new ModerationOutcome(ModerationStatus.APPROVED, "");
            }

            LOG.warn("Hugging Face image moderation could not determine a usable score: {}", response.getBody());
            return new ModerationOutcome(ModerationStatus.REVIEW_REQUIRED, REVIEW_REASON_MISSING_RESULTS);
        } catch (HttpStatusCodeException ex) {
            LOG.warn("Hugging Face image moderation HTTP error {} body={}", ex.getStatusCode(), ex.getResponseBodyAsString());
            return new ModerationOutcome(ModerationStatus.REVIEW_REQUIRED, REVIEW_REASON_HTTP_ERROR);
        } catch (IllegalArgumentException | RestClientException | com.fasterxml.jackson.core.JsonProcessingException ex) {
            LOG.warn("Hugging Face image moderation failed: {}", ex.getMessage());
            return new ModerationOutcome(ModerationStatus.REVIEW_REQUIRED, REVIEW_REASON_CLIENT_ERROR);
        }
    }

    private ImagePayload loadImagePayload(String mediaUrl) {
        String trimmed = mediaUrl == null ? "" : mediaUrl.trim();
        if (trimmed.isBlank()) {
            return null;
        }

        if (trimmed.startsWith("data:image/")) {
            return decodeDataImage(trimmed);
        }

        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            return fetchRemoteImage(trimmed);
        }

        return null;
    }

    private ImagePayload decodeDataImage(String mediaUrl) {
        int commaIndex = mediaUrl.indexOf(',');
        if (commaIndex < 0) {
            throw new IllegalArgumentException("Invalid data URL");
        }

        String metadata = mediaUrl.substring(5, commaIndex);
        String payload = mediaUrl.substring(commaIndex + 1);
        if (!metadata.contains(";base64")) {
            throw new IllegalArgumentException("Only base64 data URLs are supported");
        }

        String contentType = metadata.replace(";base64", "");
        byte[] bytes = Base64.getDecoder().decode(payload);
        return new ImagePayload(bytes, contentType);
    }

    private ImagePayload fetchRemoteImage(String mediaUrl) {
        ResponseEntity<byte[]> response = restTemplate.exchange(mediaUrl, HttpMethod.GET, HttpEntity.EMPTY, byte[].class);
        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null || response.getBody().length == 0) {
            throw new IllegalArgumentException("Remote image could not be downloaded");
        }

        String contentType = response.getHeaders().getContentType() != null
                ? response.getHeaders().getContentType().toString()
                : URLConnection.guessContentTypeFromName(mediaUrl);
        if (contentType == null || !contentType.startsWith("image/")) {
            contentType = "image/jpeg";
        }

        return new ImagePayload(response.getBody(), contentType);
    }

    private ModelScores queryModelScores(ImagePayload imagePayload, String endpoint) throws com.fasterxml.jackson.core.JsonProcessingException {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(imagePayload.contentType()));
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        headers.setBearerAuth(huggingFaceApiKey);

        HttpEntity<byte[]> entity = new HttpEntity<>(imagePayload.bytes(), headers);
        ResponseEntity<String> response = restTemplate.postForEntity(endpoint, entity, String.class);
        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null || response.getBody().isBlank()) {
            LOG.warn("Hugging Face image moderation returned status {} with empty body={} for endpoint={}", response.getStatusCode(), response.getBody() == null, endpoint);
            throw new IllegalArgumentException("Empty moderation response");
        }

        JsonNode root = objectMapper.readTree(response.getBody());
        if (!root.isArray() || root.isEmpty()) {
            LOG.warn("Hugging Face image moderation response missing results for endpoint={}: {}", endpoint, response.getBody());
            throw new IllegalArgumentException("Incomplete moderation response");
        }

        Map<String, Double> scores = new HashMap<>();
        for (JsonNode item : root) {
            String label = item.path("label").asText("").trim().toLowerCase(Locale.ROOT);
            double score = item.path("score").asDouble(-1);
            if (!label.isBlank()) {
                scores.put(label, score);
            }
        }

        return new ModelScores(scores);
    }

    public Map<String, Object> getRuntimeStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("enabled", imageModerationEnabled);
        status.put("provider", moderationProvider);
        status.put("model", huggingFaceModel);
        status.put("contextModel", huggingFaceContextModel);
        status.put("apiKeyConfigured", huggingFaceApiKey != null && !huggingFaceApiKey.isBlank());
        status.put("endpoint", huggingFaceEndpoint);
        status.put("contextEndpoint", huggingFaceContextEndpoint);
        status.put("rejectThreshold", rejectThreshold);
        status.put("reviewThreshold", reviewThreshold);
        return status;
    }

    private boolean looksLikeImage(String mediaUrl) {
        if (mediaUrl == null || mediaUrl.isBlank()) {
            return false;
        }

        String lower = mediaUrl.trim().toLowerCase(Locale.ROOT);
        if (lower.startsWith("data:image/")) {
            return true;
        }

        return lower.endsWith(".png")
                || lower.endsWith(".jpg")
                || lower.endsWith(".jpeg")
                || lower.endsWith(".gif")
                || lower.endsWith(".webp")
                || lower.contains("giphy.com")
                || lower.contains("tenor.com");
    }

    private double extractLabelScore(JsonNode root, String expectedLabel) {
        if (root == null || !root.isArray()) {
            return -1;
        }

        for (JsonNode item : root) {
            String label = item.path("label").asText("");
            if (expectedLabel.equalsIgnoreCase(label)) {
                return item.path("score").asDouble(-1);
            }
        }

        return -1;
    }

    private double maxScore(double... scores) {
        double max = -1;
        for (double score : scores) {
            if (score > max) {
                max = score;
            }
        }
        return max;
    }

    private record ImagePayload(byte[] bytes, String contentType) {
    }

    private record ModelScores(Map<String, Double> scores) {
        double score(String label) {
            return scores.getOrDefault(label.toLowerCase(Locale.ROOT), -1d);
        }
    }
}
