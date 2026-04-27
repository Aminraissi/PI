package org.example.gestionreclamation.service;

public record TextModerationResult(
        boolean allowed,
        String reason,
        String source
) {
    public static TextModerationResult allowed(String source) {
        return new TextModerationResult(true, "", source);
    }

    public static TextModerationResult blocked(String reason, String source) {
        return new TextModerationResult(false, reason, source);
    }
}
