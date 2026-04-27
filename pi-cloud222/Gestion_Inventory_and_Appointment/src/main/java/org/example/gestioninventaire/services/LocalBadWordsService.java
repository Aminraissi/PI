package org.example.gestioninventaire.services;

import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
public class LocalBadWordsService {

    private static final List<String> BAD_WORDS = List.of(
            // --- ANGLAIS ---
            "fuck", "fucking", "fucker", "faggot", "shit", "bitch", "asshole", "bastard",
            "dick", "pussy", "cunt", "motherfucker", "cock", "slut", "whore", "dumbass",
            "piss", "bollocks", "wanker", "prick", "nigger", "twat", "douchebag",
            "jackass", "dipstick", "scumbag", "skank", "tit", "clit", "cum", "retard",

            // --- FRANÇAIS (Insultes & Vulgarités) ---
            "merde", "connard", "conne", "connasse", "salope", "pute", "putain",
            "encule", "enculer", "batard", "trouduc", "trouducul", "crevard", "debile",
            "abruti", "bordel", "couille", "couilles", "pd", "poufiasse", "salaud",
            "salopard", "con", "nique", "niquer", "petasse", "teuch", "chier", "chiant",
            "gueule", "abruti", "cretin", "mongol", "pd", "pedale", "lopette", "tarlouze",
            "bordel", "bouffon", "branleur", "branleuse", "casse-couille", "clochard",

            // --- ANATOMIE & SEXE ---
            "bite", "zizi", "sexe", "vagin", "penis", "couille", "nichons", "boobs",
            "zeub", "smecta", "sperme", "orgasme", "porno", "porn",

            // --- ARABE (Transcription FR courante) ---
            "zebi", "zabour", "kahba", "kalb", "zamel", "khra", "tize", "mok",
            "din mok", "nordin", "moche", "teuchi", "shouf", "hmar", "ghab",

            // --- VARIATIONS DE CONTOURNEMENT ---
            "p.u.t.e", "s.a.l.o.p.e", "m.e.r.d.e", "f.u.c.k", "sh!t"
    );

    public TextModerationResult moderate(String text) {
        String normalized = normalize(text);
        if (normalized.isBlank()) {
            return TextModerationResult.allowed("local");
        }

        for (String badWord : BAD_WORDS) {
            Pattern pattern = Pattern.compile("(^|\\W)" + Pattern.quote(badWord) + "(\\W|$)");
            if (pattern.matcher(normalized).find()) {
                return TextModerationResult.blocked(
                        "Le contenu contient un mot inapproprie.",
                        "local"
                );
            }
        }

        return TextModerationResult.allowed("local");
    }

    private String normalize(String text) {
        if (text == null) {
            return "";
        }

        String value = Normalizer.normalize(text, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);

        value = value
                .replace('0', 'o')
                .replace('1', 'i')
                .replace('3', 'e')
                .replace('4', 'a')
                .replace('5', 's')
                .replace('7', 't');

        return value.replaceAll("[_\\-.]+", " ");
    }
}
