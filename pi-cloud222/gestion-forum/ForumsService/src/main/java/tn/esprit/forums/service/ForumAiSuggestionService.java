package tn.esprit.forums.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import tn.esprit.forums.dto.AiModerationAnalysisResponse;

@Service
public class ForumAiSuggestionService {

    public record PostPublicationReview(boolean allowed, String reason) {
    }

    private record ModerationDecision(boolean allowed, String reason) {
    }

    public record DuplicateCandidateData(Long id, String title, List<String> tags, int replies, int views, String createdAt) {
    }

    public record DuplicateCandidateMatch(Long id, int score, String reason) {
    }

    private static final Logger LOG = LoggerFactory.getLogger(ForumAiSuggestionService.class);

    private static final Set<String> AGRI_KEYWORDS = Set.of(
            "agriculture", "farmer", "farm", "crop", "soil", "irrigation", "fertilizer", "fertiliser",
            "seed", "harvest", "livestock", "veterinary", "greenhouse", "hydroponic", "pesticide",
        "compost", "plant", "disease", "yield", "weather", "drip", "tractor", "beja", "sfax", "tunis",
        "tunisia", "wheat", "barley", "olive", "date", "harvesting", "heavest"
    );

    private static final Set<String> AGRI_HINT_TERMS = Set.of(
        "field", "fields", "cereal", "cereals", "orchard", "rotation", "rainfed", "rain-fed",
        "sowing", "planting", "plowing", "ploughing", "fertigation", "mulch", "pruning"
    );

    private static final Set<String> NON_AGRI_BLOCK_TERMS = Set.of(
            "brownie", "brownies", "cake", "cookies", "cookie", "dessert", "recipe", "recipes",
            "baking", "bake", "oven", "chocolate", "cocoa", "sugar", "flour", "butter", "vanilla",
            "pizza", "pasta", "burger", "kitchen", "cook", "cooking", "restaurant", "cinema", "movie",
            "football", "gaming", "javascript", "python", "react", "angular"
    );

    private static final String MODERATION_BLOCK_MESSAGE =
            "This post violates community guidelines. Please remove the offensive or unsafe language and try again.";

    private static final Pattern ARABIC_CHAR_PATTERN = Pattern.compile("\\p{InArabic}");

    private static final Set<String> FRENCH_HINT_TERMS = Set.of(
            "bonjour", "salut", "comment", "pourquoi", "quelle", "quels", "agriculture", "agricole",
            "ferme", "irrigation", "culture", "semis", "récolte", "engrais", "pesticide", "sol", "tunisie"
    );

    private enum UserLanguage {
        EN,
        FR,
        AR
    }

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${forums.ai.enabled:true}")
    private boolean aiEnabled;

    @Value("${forums.ai.gemini.api-key:}")
    private String geminiApiKey;

    @Value("${forums.ai.gemini.model:gemini-2.0-flash}")
    private String geminiModel;

    public ForumAiSuggestionService(
            @Qualifier("externalRestTemplate") RestTemplate restTemplate,
            ObjectMapper objectMapper
    ) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    public Optional<String> generateSuggestion(String title, String content, List<String> tags) {
        return generateSuggestion(title, content, tags, null, null, List.of(), List.of());
    }

    public PostPublicationReview reviewPostForPublishing(
            String title,
            String content,
            List<String> tags,
            String groupName,
            List<String> groupRules
    ) {
        return reviewContentForPublishing(title, content, tags, groupName, groupRules, "post");
    }

    public PostPublicationReview reviewContentForPublishing(
            String title,
            String content,
            List<String> tags,
            String groupName,
            List<String> groupRules,
            String contentLabel
    ) {
        String safeTitle = title == null ? "" : title.trim();
        String safeContent = content == null ? "" : content.trim();
        String rawText = (safeTitle + "\n" + safeContent).trim();
        String normalizedText = normalizeForModeration(rawText);
        String safeContentLabel = (contentLabel == null || contentLabel.isBlank()) ? "content" : contentLabel.trim().toLowerCase();

        if (rawText.isBlank()) {
            return new PostPublicationReview(true, "");
        }

        if (containsHarmfulSignals(normalizedText)) {
            return new PostPublicationReview(false, buildModerationBlockMessage(safeContentLabel));
        }

        if (hasStrongNonAgricultureSignals(normalizedText) && !hasAgricultureSignals(normalizedText)) {
            return new PostPublicationReview(false,
                    buildScopeBlockMessage(safeContentLabel));
        }

        if (aiEnabled && geminiApiKey != null && !geminiApiKey.isBlank()) {
            String safeGroupName = groupName == null ? "" : groupName.trim();
            List<String> safeRules = groupRules == null ? List.of() : groupRules;
            String rulesText = safeRules.isEmpty() ? "none" : String.join(" | ", safeRules.stream().limit(5).toList());

            String prompt = """
                    You are a strict publication moderator for an agriculture forum.
                    Decide whether this %s can be published.

                    You must detect:
                    - toxicity, harassment, hate, slurs, threats, abuse, bullying
                    - profanity and curse words in any language
                    - censored or obfuscated profanity such as pu$$y, f***, s h i t, leetspeak, or punctuation tricks
                    - discriminatory or demeaning language targeting a person or group
                    - sexual content or explicit sexual references
                    - content that is clearly outside the agriculture forum guidelines

                    Be conservative. If the post is ambiguous or adversarial, block it.

                    Return JSON only in this exact shape:
                    {"allowed":true|false,"reason":"short user-facing reason"}

                    Community context:
                    Group name: %s
                    Group rules: %s

                    Raw title:
                    %s

                    Raw %s content:
                    %s

                    Normalized text for moderation:
                    %s
                    """.formatted(
                    safeContentLabel,
                    safeGroupName.isBlank() ? "general" : safeGroupName,
                    rulesText,
                    safeTitle,
                    safeContentLabel,
                    safeContent,
                    normalizedText.isBlank() ? "none" : normalizedText
            );

            ModerationDecision decision = parseModerationDecision(callGemini(prompt));
            if (decision != null) {
                return decision.allowed()
                        ? new PostPublicationReview(true, "")
                        : new PostPublicationReview(false, sanitizeModerationReason(decision.reason()));
            }
        }

        return buildHeuristicPublicationReview(normalizedText, safeContentLabel);
    }


    private ModerationDecision parseModerationDecision(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }

        String trimmed = raw.trim();
        String json = extractJsonObject(trimmed);
        if (json != null) {
            try {
                JsonNode root = objectMapper.readTree(json);
                if (root.isObject() && root.has("allowed")) {
                    boolean allowed = root.path("allowed").asBoolean(false);
                    String reason = root.path("reason").asText(allowed ? "" : MODERATION_BLOCK_MESSAGE).trim();
                    if (reason.isBlank() && !allowed) {
                        reason = MODERATION_BLOCK_MESSAGE;
                    }
                    return new ModerationDecision(allowed, reason);
                }
            } catch (Exception ex) {
                LOG.warn("Failed to parse moderation JSON: {}", ex.getMessage());
            }
        }

        String normalized = trimmed.toLowerCase(Locale.ROOT);
        if (normalized.startsWith("block")) {
            String reason = trimmed.contains("|") ? trimmed.substring(trimmed.indexOf('|') + 1).trim() : MODERATION_BLOCK_MESSAGE;
            return new ModerationDecision(false, reason.isBlank() ? MODERATION_BLOCK_MESSAGE : reason);
        }

        if (normalized.startsWith("allow")) {
            return new ModerationDecision(true, "");
        }

        return null;
    }

    public List<DuplicateCandidateMatch> rankDuplicateCandidates(
            String title,
            String content,
            List<String> tags,
            String groupName,
            String groupDescription,
            List<String> groupFocusTags,
            List<DuplicateCandidateData> candidates
    ) {
        String safeTitle = title == null ? "" : title;
        String safeContent = content == null ? "" : content;
        List<String> safeTags = tags == null ? List.of() : tags;
        List<DuplicateCandidateData> safeCandidates = candidates == null ? List.of() : candidates;
        if (safeCandidates.isEmpty()) {
            return List.of();
        }

        String draftText = (safeTitle + "\n" + safeContent).toLowerCase(Locale.ROOT);
        if (containsHarmfulSignals(draftText) || hasStrongNonAgricultureSignals(draftText)) {
            return List.of();
        }

        boolean canUseAi = aiEnabled && geminiApiKey != null && !geminiApiKey.isBlank() && isAgricultureRelated(draftText);

        if (canUseAi) {
            String groupContext = buildGroupContext(
                    groupName == null ? "" : groupName,
                    groupDescription == null ? "" : groupDescription,
                    groupFocusTags == null ? List.of() : groupFocusTags,
                    List.of()
            );

            StringBuilder candidateBlock = new StringBuilder();
            for (DuplicateCandidateData candidate : safeCandidates) {
                candidateBlock.append("- id: ").append(candidate.id())
                        .append(" | title: ").append(candidate.title() == null ? "" : candidate.title())
                        .append(" | tags: ").append(candidate.tags() == null || candidate.tags().isEmpty() ? "none" : String.join(", ", candidate.tags()))
                        .append(" | replies: ").append(candidate.replies())
                        .append(" | views: ").append(candidate.views())
                        .append(" | createdAt: ").append(candidate.createdAt() == null ? "" : candidate.createdAt())
                        .append("\n");
            }

            String prompt = """
                    You are ranking existing forum threads that may duplicate a new post.
                    Return a JSON array with up to 4 objects.
                    Each object must contain:
                    - id: the thread id from the candidate list
                    - score: integer 0-100, higher means more similar
                    - reason: short explanation of why this looks similar, max 20 words
                    Rules:
                    - Only use candidate ids provided below.
                    - Prefer semantic similarity over keyword matching.
                    - Be conservative: if uncertain, use low scores.
                    - Output JSON only, no markdown.

                    Community context:
                    %s

                    Draft title:
                    %s

                    Draft content:
                    %s

                    Draft tags:
                    %s

                    Candidate threads:
                    %s
                    """.formatted(
                    groupContext,
                    safeTitle,
                    safeContent,
                    safeTags.isEmpty() ? "none" : String.join(", ", safeTags),
                    candidateBlock
            );

            String response = callGemini(prompt);
            List<DuplicateCandidateMatch> parsed = parseDuplicateMatches(response, safeCandidates);
            if (!parsed.isEmpty()) {
                return parsed;
            }
        }

        return scoreDuplicateCandidatesFallback(safeTitle, safeContent, safeTags, safeCandidates);
    }

        public List<String> recommendTags(
            String title,
            String content,
            List<String> existingTags,
            String groupName,
            String groupDescription,
            List<String> groupFocusTags
        ) {
        String safeTitle = title == null ? "" : title;
        String safeContent = content == null ? "" : content;
        List<String> safeExistingTags = existingTags == null ? List.of() : existingTags;
        List<String> safeGroupFocusTags = groupFocusTags == null ? List.of() : groupFocusTags;

        String text = (safeTitle + "\n" + safeContent).toLowerCase(Locale.ROOT);
        LinkedHashSet<String> output = new LinkedHashSet<>();

        safeExistingTags.stream()
            .map(this::normalizeTag)
            .filter(this::isSafeSuggestedTag)
            .forEach(output::add);

        safeGroupFocusTags.stream()
            .map(this::normalizeTag)
            .filter(this::isSafeSuggestedTag)
            .forEach(output::add);

        if (containsHarmfulSignals(text) || hasStrongNonAgricultureSignals(text)) {
            return output.stream().limit(5).toList();
        }

        boolean canUseAi = aiEnabled && geminiApiKey != null && !geminiApiKey.isBlank() && isAgricultureRelated(text);
        if (canUseAi) {
            String groupContext = buildGroupContext(
                groupName == null ? "" : groupName,
                groupDescription == null ? "" : groupDescription,
                safeGroupFocusTags,
                List.of()
            );

            String prompt = """
                You are helping suggest forum tags for an agriculture platform.
                Return exactly one CSV line with up to 5 short tags.
                Constraints:
                - Lowercase only.
                - Use 1-2 words per tag.
                - No hashtags, no numbering, no explanations.
                - Keep tags practical and specific to the post.

                Community context:
                %s

                Post title:
                %s

                Post content:
                %s
                """.formatted(groupContext, safeTitle, safeContent);

            String aiRaw = callGemini(prompt);
                parseSuggestedTags(aiRaw).stream()
                    .filter(this::isSafeSuggestedTag)
                    .forEach(output::add);
        }

        inferTagsFromText(text).forEach(output::add);

        List<String> result = output.stream()
            .map(this::normalizeTag)
            .filter(tag -> !tag.isBlank())
            .distinct()
            .limit(5)
            .toList();

        if (result.isEmpty()) {
            return List.of("general");
        }
        return result;
        }

    public Optional<String> generateSuggestion(
            String title,
            String content,
            List<String> tags,
            String groupName,
            String groupDescription,
            List<String> groupFocusTags,
            List<String> groupRules
    ) {
        String safeTitle = title == null ? "" : title;
        String safeContent = content == null ? "" : content;
        List<String> safeTags = tags == null ? List.of() : tags;
        String safeGroupName = groupName == null ? "" : groupName.trim();
        String safeGroupDescription = groupDescription == null ? "" : groupDescription.trim();
        List<String> safeGroupFocusTags = groupFocusTags == null ? List.of() : groupFocusTags;
        List<String> safeGroupRules = groupRules == null ? List.of() : groupRules;

        String text = safeTitle + "\n" + safeContent + "\nTags: " + String.join(", ", safeTags);
        UserLanguage userLanguage = detectLanguage(text);

        if (!aiEnabled) {
            LOG.debug("Forum AI skipped: forums.ai.enabled=false");
            return Optional.of(buildTemporaryUnavailableMessage(userLanguage));
        }

        if (geminiApiKey == null || geminiApiKey.isBlank()) {
            LOG.warn("Forum AI skipped: GEMINI_API_KEY is missing or blank");
            return Optional.of(buildTemporaryUnavailableMessage(userLanguage));
        }

        if (!geminiApiKey.startsWith("AIza")) {
            LOG.warn("Forum AI key format looks unusual (expected prefix AIza). Check copied key.");
        }

        if (!isAgricultureRelated(text)) {
            LOG.debug("Forum AI skipped: post classified as non-agriculture");
            return Optional.of(buildOutOfScopeMessage(userLanguage));
        }

        String groupContext = buildGroupContext(
            safeGroupName,
            safeGroupDescription,
            safeGroupFocusTags,
            safeGroupRules
        );

        String prompt = """
                You are an agriculture assistant for the GreenRoots forum.
                Write a helpful first reply to the user's post.
                Constraints:
                - Reply strictly in %s.
                - Stay strictly in agriculture, farming, agri-business, veterinary, logistics, or rural operations.
                - Do NOT ask follow-up questions.
                - Give a complete answer in one message, even when context is incomplete.
                - If data is missing, state your assumption briefly and continue with practical guidance.
                - Keep it concise: max 120 words.
                - Give practical steps, not generic advice.
                                - End with two short lines:
                                    Confidence: low|medium|high
                                    Assumption: <short assumption used>

                Community context:
                %s

                User post:
                """.formatted(languageInstruction(userLanguage), groupContext) + text;

        String suggestion = callGemini(prompt);
        if (suggestion == null || suggestion.isBlank()) {
            LOG.warn("Forum AI call returned empty suggestion");
            return Optional.of(buildGenerationErrorMessage(userLanguage));
        }

        // Ensure generated output matches one-shot forum insight behavior.
        if (containsQuestionTone(suggestion)) {
            String rewritePrompt = """
                    Rewrite the following assistant reply to remove all questions.
                    Keep the reply in %s.
                    Keep the meaning and keep it concise (max 120 words).
                    Output only the rewritten final answer.

                    Reply:
                    """.formatted(languageInstruction(userLanguage)) + suggestion;

            String rewritten = callGemini(rewritePrompt);
            if (rewritten != null && !rewritten.isBlank()) {
                suggestion = rewritten;
            }
        }

        return Optional.of(suggestion.trim());
    }

    public Optional<String> improveReplyDraft(
            String draft,
            String postTitle,
            String postContent,
            String groupName,
            String groupDescription,
            List<String> groupFocusTags,
            List<String> groupRules
    ) {
        String safeDraft = draft == null ? "" : draft.trim();
        if (safeDraft.isBlank()) {
            return Optional.empty();
        }

        UserLanguage userLanguage = detectLanguage(safeDraft + "\n" + (postTitle == null ? "" : postTitle));
        if (!aiEnabled || geminiApiKey == null || geminiApiKey.isBlank()) {
            return Optional.empty();
        }

        String groupContext = buildGroupContext(
                groupName == null ? "" : groupName,
                groupDescription == null ? "" : groupDescription,
                groupFocusTags == null ? List.of() : groupFocusTags,
                groupRules == null ? List.of() : groupRules
        );

        String prompt = """
                Rewrite this forum reply to improve clarity and actionability.
                Constraints:
                - Keep meaning intact, do not invent facts.
                - Keep in %s.
                - Keep agriculture-focused and practical.
                - Keep concise: max 120 words.
                - Use direct, field-ready steps.
                - Do not ask follow-up questions.
                - Output only the improved reply.

                Community context:
                %s

                Original thread title:
                %s

                Original thread context:
                %s

                Draft reply:
                %s
                """.formatted(
                languageInstruction(userLanguage),
                groupContext,
                postTitle == null ? "" : postTitle,
                postContent == null ? "" : postContent,
                safeDraft
        );

        String improved = callGemini(prompt);
        if (improved == null || improved.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(improved.trim());
    }

    public Optional<AiModerationAnalysisResponse> analyzeModerationCase(
            String title,
            String content,
            List<String> tags,
            String groupName,
            String groupDescription,
            List<String> groupFocusTags,
            List<String> groupRules
    ) {
        String safeTitle = title == null ? "" : title.trim();
        String safeContent = content == null ? "" : content.trim();
        List<String> safeTags = tags == null ? List.of() : tags;
        String text = (safeTitle + "\n" + safeContent).trim();
        String normalizedText = normalizeForModeration(text).toLowerCase(Locale.ROOT);
        if (text.isBlank()) {
            return Optional.empty();
        }

        if (containsHarmfulSignals(normalizedText)) {
            return Optional.of(new AiModerationAnalysisResponse(
                    "REJECT",
                    "HIGH",
                    "The post content contains harmful or abusive language and should not remain visible.",
                    List.of(
                            "Harmful language indicators detected in the post text",
                            "Policy risk is present even without relying on reports"
                    )
            ));
        }

        if (hasStrongNonAgricultureSignals(normalizedText) && !hasAgricultureSignals(normalizedText)) {
            return Optional.of(new AiModerationAnalysisResponse(
                    "REVIEW_CAREFULLY",
                    "MEDIUM",
                    "The post appears outside this agriculture forum scope and needs moderator verification.",
                    List.of(
                            "Detected signals that suggest non-agriculture content",
                            "No strong agriculture-specific context found"
                    )
            ));
        }

        if (!aiEnabled || geminiApiKey == null || geminiApiKey.isBlank()) {
            return Optional.of(buildHeuristicModerationAnalysis(normalizedText));
        }

        String groupContext = buildGroupContext(
                groupName == null ? "" : groupName,
                groupDescription == null ? "" : groupDescription,
                groupFocusTags == null ? List.of() : groupFocusTags,
                groupRules == null ? List.of() : groupRules
        );

        String prompt = """
                You are a strict moderator assistant for an agriculture forum.
                Analyze the post itself, not the report volume.
            Decide whether the content should be APPROVE, REVIEW_CAREFULLY, or REJECT.

                Use this rubric:
                - REJECT for hate, abuse, threats, harassment, profanity, slurs, sexual content, or content that clearly violates forum rules.
                - REVIEW_CAREFULLY when the content is ambiguous, borderline, or potentially off-topic.
                - APPROVE only when the content is clearly acceptable.

                Be conservative. If in doubt, do not approve.

                Return JSON only in this exact shape:
                {"recommendation":"APPROVE|REVIEW_CAREFULLY|REJECT","confidence":"LOW|MEDIUM|HIGH","rationale":"short reason","signals":["specific signal","specific signal"]}

                Rules for signals:
                - Provide 2 to 4 concrete signals.
                - Do NOT output placeholders such as "signal 1".

                Community context:
                %s

                Post title:
                %s

                Post content:
                %s

                Post tags:
                %s
                """.formatted(
                groupContext,
                safeTitle,
                safeContent,
                safeTags.isEmpty() ? "none" : String.join(", ", safeTags)
        );

        String response = callGemini(prompt);
        Optional<AiModerationAnalysisResponse> parsed = parseAiModerationAnalysis(response);
        if (parsed.isEmpty() && response != null && !response.isBlank()) {
            String normalizePrompt = """
                    Convert the following moderation output into strict JSON.
                    Return only valid JSON in this exact shape:
                    {"recommendation":"APPROVE|REVIEW_CAREFULLY|REJECT","confidence":"LOW|MEDIUM|HIGH","rationale":"short reason","signals":["specific signal","specific signal"]}

                    Output to normalize:
                    %s
                    """.formatted(response);
            parsed = parseAiModerationAnalysis(callGemini(normalizePrompt));
        }

        if (parsed.isPresent()) {
            AiModerationAnalysisResponse analysis = parsed.get();
            if (containsHarmfulSignals(normalizedText) && !"REJECT".equals(analysis.recommendation())) {
                return Optional.of(new AiModerationAnalysisResponse(
                        "REJECT",
                        "HIGH",
                        "The post contains explicit abusive language, so rejection is safer.",
                        List.of(
                                "Direct harmful language was detected in post text",
                                "Recommendation overridden by safety guardrail"
                        )
                ));
            }
            return Optional.of(analysis);
        }

        return Optional.of(buildHeuristicModerationAnalysis(normalizedText));
    }

    private Optional<AiModerationAnalysisResponse> parseAiModerationAnalysis(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }

        String json = extractJsonObject(raw.trim());
        if (json == null) {
            return Optional.empty();
        }

        try {
            JsonNode root = objectMapper.readTree(json);
            if (!root.isObject()) {
                return Optional.empty();
            }

            String recommendation = root.path("recommendation").asText("REVIEW_CAREFULLY").trim().toUpperCase(Locale.ROOT);
            if (recommendation.equals("REJECTED")) {
                recommendation = "REJECT";
            }
            if (recommendation.equals("APPROVED")) {
                recommendation = "APPROVE";
            }
            if (!recommendation.equals("APPROVE") && !recommendation.equals("REVIEW_CAREFULLY") && !recommendation.equals("REJECT")) {
                recommendation = "REVIEW_CAREFULLY";
            }

            String confidence = root.path("confidence").asText("MEDIUM").trim().toUpperCase(Locale.ROOT);
            if (!confidence.equals("LOW") && !confidence.equals("MEDIUM") && !confidence.equals("HIGH")) {
                confidence = "MEDIUM";
            }

            String rationale = root.path("rationale").asText("No rationale provided.").trim();
            if (rationale.isBlank()) {
                rationale = "No rationale provided.";
            }

            List<String> signals = new ArrayList<>();
            JsonNode signalsNode = root.path("signals");
            if (signalsNode.isArray()) {
                for (JsonNode signal : signalsNode) {
                    String value = signal.asText("").trim();
                    if (!value.isBlank() && !value.matches("(?i)^signal\\s*\\d+$")) {
                        signals.add(value);
                    }
                }
            }

            if (signals.isEmpty()) {
                signals.add("AI analyzed the post content directly");
            }

            return Optional.of(new AiModerationAnalysisResponse(recommendation, confidence, rationale, signals.stream().limit(4).toList()));
        } catch (Exception ex) {
            LOG.warn("Failed to parse moderation analysis: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    private AiModerationAnalysisResponse buildHeuristicModerationAnalysis(String normalizedText) {
        if (hasAgricultureSignals(normalizedText) && !hasStrongNonAgricultureSignals(normalizedText) && !containsHarmfulSignals(normalizedText)) {
            return new AiModerationAnalysisResponse(
                    "APPROVE",
                    "MEDIUM",
                    "The post appears agriculture-related and does not show direct harmful language in its content.",
                    List.of(
                            "Agriculture context is present in the post text",
                            "No direct harmful language indicators detected"
                    )
            );
        }

        return new AiModerationAnalysisResponse(
                "REVIEW_CAREFULLY",
                "MEDIUM",
                "Automated moderation could not produce a definitive verdict; manual review is recommended.",
                List.of(
                        "Classifier output was inconclusive",
                        "Fallback moderation review"
                )
        );
    }

    private PostPublicationReview buildHeuristicPublicationReview(String normalizedText, String contentLabel) {
        if (containsHarmfulSignals(normalizedText)) {
            return new PostPublicationReview(false, buildModerationBlockMessage(contentLabel));
        }

        if (hasStrongNonAgricultureSignals(normalizedText) && !hasAgricultureSignals(normalizedText)) {
            return new PostPublicationReview(false,
                    buildScopeBlockMessage(contentLabel));
        }

        return new PostPublicationReview(true, "");
    }

    private String buildModerationBlockMessage(String contentLabel) {
        return "This %s violates community guidelines. Please remove the offensive or unsafe language and try again."
                .formatted(contentLabel);
    }

    private String buildScopeBlockMessage(String contentLabel) {
        return "This %s appears outside the agriculture forum scope. Please keep it focused on farming or rural operations."
                .formatted(contentLabel);
    }

        private String buildGroupContext(
            String groupName,
            String groupDescription,
            List<String> groupFocusTags,
            List<String> groupRules
        ) {
        if (groupName == null || groupName.isBlank()) {
            return "No specific group context provided. Reply with general agriculture guidance.";
        }

        String tagsText = groupFocusTags == null || groupFocusTags.isEmpty()
            ? "none"
            : String.join(", ", groupFocusTags);

        String rulesText = groupRules == null || groupRules.isEmpty()
            ? "none"
            : String.join(" | ", groupRules.stream().limit(4).toList());

        String descriptionText = groupDescription == null || groupDescription.isBlank()
            ? "No description provided."
            : groupDescription;

        return "Group: " + groupName + "\n"
            + "Description: " + descriptionText + "\n"
            + "Focus tags: " + tagsText + "\n"
            + "Rules to respect: " + rulesText + "\n"
            + "Prioritize advice aligned with this group's focus and rules.";
        }

    public Map<String, Object> getRuntimeStatus() {
        String key = geminiApiKey == null ? "" : geminiApiKey.trim();
        String prefix = key.length() >= 4 ? key.substring(0, 4) + "***" : "";

        Map<String, Object> status = new LinkedHashMap<>();
        status.put("enabled", aiEnabled);
        status.put("model", geminiModel);
        status.put("apiKeyPresent", !key.isBlank());
        status.put("apiKeyPrefix", prefix);
        return status;
    }

    private boolean isAgricultureRelated(String input) {
        String lower = input.toLowerCase(Locale.ROOT);
        if (hasAgricultureSignals(lower)) {
            return true;
        }

        // Guardrail: reject clearly unrelated content (recipes, coding, entertainment, etc.)
        // before calling the LLM classifier.
        if (hasStrongNonAgricultureSignals(lower)) {
            return false;
        }

        // TODO: Add stronger policy checks or moderation rules when product requirements are finalized.
        String classifierPrompt = """
                Classify this forum post for an agriculture platform.
                You must output exactly one token:
                - ALLOW_AGRI (only if the post is clearly about agriculture/farming/veterinary/rural operations)
                - DENY_NON_AGRI (for any other topic)
                Be conservative: if uncertain, return DENY_NON_AGRI.

                Post:
                """ + input;

        String verdict = callGemini(classifierPrompt);
        if (verdict == null || verdict.isBlank()) {
            return false;
        }

        String normalizedVerdict = verdict
                .trim()
                .toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z]", "");
        return normalizedVerdict.contains("ALLOWAGRI");
    }

    private boolean hasAgricultureSignals(String lower) {
        return AGRI_KEYWORDS.stream().anyMatch(lower::contains)
                || AGRI_HINT_TERMS.stream().anyMatch(lower::contains);
    }

    private boolean hasStrongNonAgricultureSignals(String lower) {
        return NON_AGRI_BLOCK_TERMS.stream().anyMatch(lower::contains);
    }

    private String callGemini(String prompt) {
        try {
            String url = "https://generativelanguage.googleapis.com/v1beta/models/"
                    + geminiModel
                    + ":generateContent?key="
                    + geminiApiKey;

            String body = objectMapper.writeValueAsString(
                    java.util.Map.of(
                            "contents", List.of(
                                    java.util.Map.of(
                                            "parts", List.of(java.util.Map.of("text", prompt))
                                    )
                            )
                    )
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(body, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                LOG.warn("Gemini call failed with status {} and empty body={}", response.getStatusCode(), response.getBody() == null);
                return null;
            }

            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode textNode = root.path("candidates").path(0).path("content").path("parts").path(0).path("text");
            if (textNode.isMissingNode()) {
                LOG.warn("Gemini response missing text candidate: {}", response.getBody());
                return null;
            }
            return textNode.asText();
        } catch (HttpStatusCodeException ex) {
            LOG.warn("Gemini HTTP error {} body={}", ex.getStatusCode(), ex.getResponseBodyAsString());
            return null;
        } catch (RestClientException | com.fasterxml.jackson.core.JsonProcessingException ex) {
            LOG.warn("Gemini call exception: {}", ex.getMessage());
            return null;
        }
    }

    private List<String> parseSuggestedTags(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }

        String normalized = raw
                .replace("\n", ",")
                .replace(";", ",")
                .replace("|", ",");

        LinkedHashSet<String> parsed = new LinkedHashSet<>();
        for (String token : normalized.split(",")) {
            String candidate = normalizeTag(token);
            if (isSafeSuggestedTag(candidate)) {
                parsed.add(candidate);
            }
        }

        return parsed.stream().limit(5).toList();
    }

    private List<String> inferTagsFromText(String lowerText) {
        if (lowerText == null || lowerText.isBlank()) {
            return List.of("general");
        }

        List<String> inferred = new ArrayList<>();

        if (containsAny(lowerText, "irrigation", "drip", "water", "watering")) {
            inferred.add("irrigation");
        }
        if (containsAny(lowerText, "soil", "compost", "ph", "organic matter")) {
            inferred.add("soil");
        }
        if (containsAny(lowerText, "disease", "fungus", "mildew", "blight", "infection")) {
            inferred.add("disease");
        }
        if (containsAny(lowerText, "pest", "insect", "aphid", "worm", "beetle")) {
            inferred.add("pest");
        }
        if (containsAny(lowerText, "fertilizer", "fertiliser", "npk", "nitrogen", "phosphorus", "potassium")) {
            inferred.add("fertilizer");
        }
        if (containsAny(lowerText, "harvest", "yield", "post-harvest", "storage")) {
            inferred.add("harvest");
        }
        if (containsAny(lowerText, "greenhouse", "tunnel", "hydroponic")) {
            inferred.add("greenhouse");
        }
        if (containsAny(lowerText, "wheat", "barley", "cereal")) {
            inferred.add("wheat");
        }

        if (inferred.isEmpty()) {
            inferred.add("general");
        }
        return inferred;
    }

    private List<DuplicateCandidateMatch> parseDuplicateMatches(String raw, List<DuplicateCandidateData> candidates) {
        if (raw == null || raw.isBlank() || candidates == null || candidates.isEmpty()) {
            return List.of();
        }

        String trimmed = raw.trim();
        int start = trimmed.indexOf('[');
        int end = trimmed.lastIndexOf(']');
        if (start < 0 || end <= start) {
            return List.of();
        }

        String json = trimmed.substring(start, end + 1);
        try {
            JsonNode root = objectMapper.readTree(json);
            if (!root.isArray()) {
                return List.of();
            }

            Map<Long, DuplicateCandidateData> candidateMap = new LinkedHashMap<>();
            candidates.forEach(candidate -> candidateMap.put(candidate.id(), candidate));

            List<DuplicateCandidateMatch> matches = new ArrayList<>();
            for (JsonNode item : root) {
                Long id = item.path("id").isNumber() ? item.path("id").asLong() : null;
                if (id == null || !candidateMap.containsKey(id)) {
                    continue;
                }

                int score = item.path("score").isInt() ? item.path("score").asInt() : 0;
                score = Math.max(0, Math.min(100, score));
                String reason = item.path("reason").asText("").trim();
                matches.add(new DuplicateCandidateMatch(id, score, reason));
            }

            matches.sort((a, b) -> Integer.compare(b.score(), a.score()));
            return matches.stream().limit(4).toList();
        } catch (Exception ex) {
            LOG.warn("Failed to parse duplicate candidate ranking: {}", ex.getMessage());
            return List.of();
        }
    }

    private List<DuplicateCandidateMatch> scoreDuplicateCandidatesFallback(
            String title,
            String content,
            List<String> tags,
            List<DuplicateCandidateData> candidates
    ) {
        String text = (title + "\n" + content).toLowerCase(Locale.ROOT);
        Set<String> draftTokens = tokenizeForSimilarity(text);
        Set<String> draftTags = new LinkedHashSet<>();
        for (String tag : tags) {
            String normalized = normalizeTag(tag);
            if (!normalized.isBlank()) {
                draftTags.add(normalized);
            }
        }

        return candidates.stream()
                .map(candidate -> {
                    Set<String> candidateTokens = tokenizeForSimilarity((candidate.title() == null ? "" : candidate.title()) + "\n" + String.join(" ", candidate.tags() == null ? List.of() : candidate.tags()));
                    long sharedTokens = draftTokens.stream().filter(candidateTokens::contains).count();
                    int unionSize = new LinkedHashSet<>(draftTokens) {{ addAll(candidateTokens); }}.size();
                    double textSimilarity = unionSize == 0 ? 0.0 : (double) sharedTokens / unionSize;

                    Set<String> candidateTagSet = new LinkedHashSet<>();
                    if (candidate.tags() != null) {
                        candidate.tags().forEach(tag -> {
                            String normalized = normalizeTag(tag);
                            if (!normalized.isBlank()) {
                                candidateTagSet.add(normalized);
                            }
                        });
                    }
                    long sharedTags = draftTags.stream().filter(candidateTagSet::contains).count();
                    double tagSimilarity = draftTags.isEmpty() ? 0.0 : (double) sharedTags / draftTags.size();

                    double recencyBoost = 0.0;
                    if (candidate.createdAt() != null && candidate.createdAt().length() >= 4) {
                        recencyBoost = 0.06;
                    }

                    int score = (int) Math.round(Math.min(100.0, (textSimilarity * 72.0) + (tagSimilarity * 22.0) + recencyBoost * 100.0));
                    String reason;
                    if (tagSimilarity >= 0.4) {
                        reason = "Shares key tags and likely the same farming topic";
                    } else if (textSimilarity >= 0.25) {
                        reason = "Uses similar wording and describes a related problem";
                    } else {
                        reason = "Light semantic overlap with a related forum thread";
                    }
                    return new DuplicateCandidateMatch(candidate.id(), score, reason);
                })
                .filter(match -> match.score() >= 35)
                .sorted((a, b) -> Integer.compare(b.score(), a.score()))
                .limit(4)
                .toList();
    }

    private Set<String> tokenizeForSimilarity(String text) {
        String normalized = (text == null ? "" : text.toLowerCase(Locale.ROOT))
                .replaceAll("[^a-z0-9\\s]", " ");
        Set<String> tokens = new LinkedHashSet<>();
        for (String token : normalized.split("\\s+")) {
            String trimmed = token.trim();
            if (trimmed.length() >= 4) {
                tokens.add(trimmed);
            }
        }
        return tokens;
    }

    private boolean containsAny(String text, String... terms) {
        for (String term : terms) {
            if (text.contains(term)) {
                return true;
            }
        }
        return false;
    }

    private String normalizeTag(String input) {
        if (input == null) {
            return "";
        }

        String normalized = input
                .trim()
                .toLowerCase(Locale.ROOT)
                .replace("#", "")
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", " ")
                .trim();

        if (normalized.length() > 24) {
            normalized = normalized.substring(0, 24).trim();
        }
        return normalized;
    }

    private boolean isSafeSuggestedTag(String tag) {
        if (tag == null || tag.isBlank()) {
            return false;
        }

        String lower = tag.toLowerCase(Locale.ROOT);
        if (containsAny(lower, "hate", "hateful", "racist", "sexist", "harass", "abuse", "violent", "violence", "threat", "kill", "die", "attack", "insult")) {
            return false;
        }

        if (containsAny(lower, "brownie", "brownies", "cake", "cookies", "cookie", "dessert", "recipe", "recipes", "baking", "bake", "oven", "chocolate", "cocoa", "sugar", "flour", "butter", "vanilla", "pizza", "pasta", "burger", "kitchen", "cook", "cooking", "restaurant", "cinema", "movie", "football", "gaming", "javascript", "python", "react", "angular")) {
            return false;
        }

        return true;
    }

    private boolean containsHarmfulSignals(String input) {
        if (input == null || input.isBlank()) {
            return false;
        }

        String normalized = normalizeForModeration(input).toLowerCase(Locale.ROOT);
        return containsAny(normalized,
                "hate", "hateful", "racist", "sexist", "harass", "harassment", "abuse", "abusive",
                "violent", "violence", "threat", "threaten", "die", "kill", "attack", "insult",
                "stupid", "idiot", "moron", "leech", "parasite", "worthless", "trash", "garbage", "slur");
    }

    private String normalizeForModeration(String input) {
        if (input == null || input.isBlank()) {
            return "";
        }

        StringBuilder builder = new StringBuilder(input.length());
        for (int index = 0; index < input.length(); index++) {
            char current = Character.toLowerCase(input.charAt(index));
            builder.append(simplifyModerationCharacter(current));
        }

        return builder.toString()
                .replaceAll("[^\\p{L}\\p{Nd}]+", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private char simplifyModerationCharacter(char current) {
        return switch (current) {
            case '$' -> 's';
            case '@' -> 'a';
            case '0' -> 'o';
            case '1', '!' -> 'i';
            case '2' -> 'z';
            case '3' -> 'e';
            case '4' -> 'a';
            case '5' -> 's';
            case '6' -> 'g';
            case '7' -> 't';
            case '8' -> 'b';
            case '9' -> 'g';
            case '|' -> 'i';
            default -> current;
        };
    }

    private String sanitizeModerationReason(String reason) {
        if (reason == null || reason.isBlank()) {
            return MODERATION_BLOCK_MESSAGE;
        }

        return reason.trim();
    }

    private String extractJsonObject(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }

        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start < 0 || end <= start) {
            return null;
        }

        return text.substring(start, end + 1);
    }

    private boolean containsQuestionTone(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }

        String normalized = text.toLowerCase(Locale.ROOT);
        return normalized.contains("?")
                || normalized.contains("could you")
                || normalized.contains("can you")
                || normalized.contains("would you")
                || normalized.contains("please tell");
    }

    private UserLanguage detectLanguage(String text) {
        if (text == null || text.isBlank()) {
            return UserLanguage.EN;
        }

        String normalized = text.trim();
        String lower = normalized.toLowerCase(Locale.ROOT);

        if (ARABIC_CHAR_PATTERN.matcher(normalized).find()) {
            return UserLanguage.AR;
        }

        int frenchScore = 0;
        if (lower.matches(".*[éèêëàâîïôùûç].*")) {
            frenchScore += 2;
        }
        if (FRENCH_HINT_TERMS.stream().anyMatch(lower::contains)) {
            frenchScore += 2;
        }
        if (lower.contains(" je ") || lower.contains(" vous ") || lower.contains(" avec ") || lower.contains(" pour ")) {
            frenchScore += 1;
        }

        if (frenchScore >= 2) {
            return UserLanguage.FR;
        }

        return UserLanguage.EN;
    }

    private String languageInstruction(UserLanguage language) {
        return switch (language) {
            case FR -> "French";
            case AR -> "Arabic";
            default -> "English";
        };
    }

    private String buildOutOfScopeMessage(UserLanguage language) {
        return switch (language) {
            case FR -> "Je peux seulement aider sur des sujets agricoles (agriculture, élevage, irrigation, logistique rurale ou activités liées). Cette question semble hors périmètre. Reformulez-la avec un contexte agricole et je vous aiderai avec plaisir.";
            case AR -> "أستطيع المساعدة فقط في المواضيع الزراعية مثل الزراعة وتربية الماشية والري والخدمات الريفية المرتبطة بها. هذا السؤال يبدو خارج هذا النطاق. يرجى إعادة صياغته ضمن سياق زراعي وسأساعدك بكل سرور.";
            default -> "I can only help with agriculture-related topics (farming, livestock, irrigation, agri-business, and rural operations). This question appears outside that scope. Please reframe it in an agricultural context and I will gladly help.";
        };
    }

    private String buildGenerationErrorMessage(UserLanguage language) {
        return switch (language) {
            case FR -> "Je n'ai pas pu générer une réponse complète pour le moment. Réessayez dans quelques instants. En attendant, vous pouvez ajouter plus de détails pratiques (culture, région, saison, type de sol) pour obtenir une réponse plus précise.";
            case AR -> "تعذر إنشاء رد كامل حالياً. يرجى المحاولة مرة أخرى بعد قليل. وفي الوقت الحالي يمكنك إضافة تفاصيل عملية أكثر مثل نوع المحصول والمنطقة والموسم ونوع التربة للحصول على إجابة أدق.";
            default -> "I could not generate a complete reply right now. Please try again in a moment. Meanwhile, adding practical details (crop type, region, season, soil conditions) can help produce a more precise answer.";
        };
    }

    private String buildTemporaryUnavailableMessage(UserLanguage language) {
        return switch (language) {
            case FR -> "L'assistant IA est temporairement indisponible. Veuillez réessayer dans quelques instants.";
            case AR -> "مساعد الذكاء الاصطناعي غير متاح مؤقتاً. يرجى المحاولة مرة أخرى بعد قليل.";
            default -> "The AI assistant is temporarily unavailable. Please try again shortly.";
        };
    }
}
