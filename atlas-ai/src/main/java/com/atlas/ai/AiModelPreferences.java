package com.atlas.ai;

public record AiModelPreferences(
        String resumeGeneration,
        String coverLetters,
        String applicationAnswers,
        String jobAnalysis,
        String visaReasoning,
        String codeGeneration,
        String fastFallback
) {
    public static AiModelPreferences defaults() {
        return new AiModelPreferences(
                "qwen3:4b",
                "qwen3:4b",
                "qwen3:4b",
                "qwen3:4b",
                "qwen3:4b",
                "qwen2.5-coder:3b",
                "gemma3:4b"
        );
    }

    public String modelFor(String task) {
        if (task == null || task.isBlank()) {
            return resumeGeneration;
        }
        return switch (task) {
            case "resume", "resumeGeneration", "applicationPackage" -> resumeGeneration;
            case "coverLetter", "coverLetters" -> coverLetters;
            case "applicationAnswers", "answers" -> applicationAnswers;
            case "jobAnalysis", "match" -> jobAnalysis;
            case "visaReasoning", "visa" -> visaReasoning;
            case "codeGeneration", "code" -> codeGeneration;
            case "fastFallback", "fallback" -> fastFallback;
            default -> resumeGeneration;
        };
    }
}
