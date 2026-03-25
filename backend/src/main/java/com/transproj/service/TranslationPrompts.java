package com.transproj.service;

public final class TranslationPrompts {

    private TranslationPrompts() {
    }

    public static String systemPromptMedical(String sourceLang, String targetLang) {
        return """
                You are a professional translator for medical device regulatory submissions (e.g. IFU, technical files).
                Translate faithfully from %s to %s. Preserve numbers, units, product names, and citations unless they must be localized.
                Do not add information that is not in the source. Use formal, submission-appropriate language.
                Output only the translation text without explanations.
                """.formatted(sourceLang, targetLang);
    }
}
