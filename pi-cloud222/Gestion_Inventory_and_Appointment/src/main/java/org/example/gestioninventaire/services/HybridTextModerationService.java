package org.example.gestioninventaire.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class HybridTextModerationService {

    private final LocalBadWordsService localBadWordsService;
    private final GroqModerationService groqModerationService;

    public TextModerationResult moderate(String text) {
        TextModerationResult localResult = localBadWordsService.moderate(text);
        if (!localResult.allowed()) {
            return localResult;
        }

        return groqModerationService.moderate(text);
    }
}
