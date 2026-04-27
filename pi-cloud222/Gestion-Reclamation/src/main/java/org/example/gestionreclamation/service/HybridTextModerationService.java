package org.example.gestionreclamation.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class HybridTextModerationService {

    private final ReclamationAiService reclamationAiService;

    public TextModerationResult moderate(String text) {
        return reclamationAiService.moderate(text);
    }
}
