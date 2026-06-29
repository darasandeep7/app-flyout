package com.atlas.career.service;

import com.atlas.ai.ChatRequest;
import com.atlas.ai.AiModelSettingsService;
import com.atlas.ai.ModelProvider;
import com.atlas.career.domain.ApplicationPackage;
import com.atlas.career.domain.ApplicationQuestionAnswer;
import com.atlas.career.domain.AnswerTrainingRule;
import com.atlas.career.domain.CareerPreferences;
import com.atlas.career.domain.JobRecord;
import com.atlas.career.domain.MasterResume;
import com.atlas.common.Slug;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.atlas.recommendation.RecommendationCategory;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class ApplicationPackageService {
    private final CareerRepository repository;
    private final ModelProvider modelProvider;
    private final AiModelSettingsService modelSettings;
    private final ObjectMapper objectMapper;

    public ApplicationPackageService(CareerRepository repository, ModelProvider modelProvider, AiModelSettingsService modelSettings, ObjectMapper objectMapper) {
        this.repository = repository;
        this.modelProvider = modelProvider;
        this.modelSettings = modelSettings;
        this.objectMapper = objectMapper.findAndRegisterModules();
    }

    public boolean shouldPrepare(JobRecord job) {
        CareerPreferences preferences = repository.preferences();
        if (containsIgnoreCase(preferences.blacklistCompanies(), job.company())) {
            return false;
        }
        if (job.match().overallMatch() < preferences.minimumMatchScore()) {
            return false;
        }
        if (preferences.visaRequired() && job.visa().score() < 50) {
            return false;
        }
        if (job.intelligence() == null || job.intelligence().recommendation() == null) {
            return job.match().overallMatch() >= preferences.minimumMatchScore();
        }
        RecommendationCategory category = job.intelligence().recommendation().category();
        return category == RecommendationCategory.APPLY_TODAY
                || category == RecommendationCategory.EXCELLENT_MATCH
                || category == RecommendationCategory.GOOD_MATCH;
    }

    public ApplicationPackage create(JobRecord job) {
        Instant now = Instant.now();
        String id = Slug.of(job.company() + "-" + job.title() + "-" + job.id());
        String folder = "applications/" + id;
        MasterResume masterResume = repository.masterResume();
        GeneratedApplicationDraft draft = draft(job, masterResume);
        List<ApplicationQuestionAnswer> answers = List.of(
                new ApplicationQuestionAnswer("Tell me about yourself.", draft.answer("tellMeAboutYourself", professionalSummary(job)), "Generated from the Master Resume and job description.", true),
                new ApplicationQuestionAnswer("Why this company?", draft.answer("whyThisCompany", "I am interested in " + job.company() + " because the role aligns with my backend engineering strengths and the company's hiring needs. I would review company-specific details before submitting."), "Draft requiring company research review.", true),
                new ApplicationQuestionAnswer("Work authorization", authorizationAnswer(job), "Generated from visa intelligence.", true),
                new ApplicationQuestionAnswer("Salary expectations", draft.answer("salaryExpectations", "Open to a competitive market-aligned package based on scope, level, and total compensation."), "Reusable saved answer.", true)
        );
        String recommendation = job.intelligence() == null ? "Review" : job.intelligence().recommendation().category().name();
        int confidence = job.intelligence() == null ? job.match().overallMatch() : job.intelligence().recommendation().confidence();
        return new ApplicationPackage(
                id,
                job.id(),
                job.company(),
                job.title(),
                "WAITING_FOR_REVIEW",
                confidence,
                recommendation,
                "master-tailored-" + id,
                folder + "/resume.pdf",
                folder + "/cover-letter.pdf",
                folder + "/answers.json",
                folder + "/application-report.md",
                answers,
                now,
                now
        );
    }

    public String resumeMarkdown(JobRecord job) {
        MasterResume masterResume = repository.masterResume();
        return draft(job, masterResume).resume();
    }

    public String coverLetterMarkdown(JobRecord job) {
        MasterResume masterResume = repository.masterResume();
        return draft(job, masterResume).coverLetter();
    }

    private GeneratedApplicationDraft draft(JobRecord job, MasterResume masterResume) {
        String fallbackResume = fallbackResume(job, masterResume);
        String fallbackCoverLetter = fallbackCoverLetter(job);
        if (masterResume.content().equals(MasterResume.empty().content())) {
            return new GeneratedApplicationDraft(fallbackResume, fallbackCoverLetter, Map.of(), job.match().overallMatch(), job.visa().reason(), 45);
        }

        String model = modelSettings.modelFor("applicationPackage");
        Path cachePath = repository.resolveCareerPath("ai-cache/application-packages/" + cacheKey(job, masterResume, model) + ".json");
        if (Files.exists(cachePath)) {
            try {
                return parseDraft(Files.readString(cachePath), fallbackResume, fallbackCoverLetter);
            } catch (Exception ignored) {
                // Regenerate below if the cache file is stale or malformed.
            }
        }

        String prompt = """
                You are Atlas Career Copilot generating one application package for Sandeep.
                Use ONLY facts from the Master Resume. Never invent employers, dates, tools, metrics, projects, education, certifications, or experience.
                Return JSON only. No markdown fence.
                Schema:
                {
                  "resume": "complete ATS-friendly tailored resume in Markdown",
                  "coverLetter": "concise cover letter under 300 words in Markdown",
                  "answers": {
                    "tellMeAboutYourself": "editable application answer",
                    "whyThisCompany": "editable application answer without invented company facts",
                    "salaryExpectations": "editable application answer"
                  },
                  "matchScore": 0,
                  "visaAnalysis": "short explanation",
                  "confidence": 0
                }

                Job:
                Company: %s
                Title: %s
                Location: %s
                Description:
                %s

                Current deterministic scores:
                Overall: %d
                Java: %d
                Spring: %d
                Backend: %d
                Visa: %d

                Master Resume:
                %s

                Saved answer training rules:
                %s
                """.formatted(job.company(), job.title(), job.location(), trim(job.description(), 7000), job.match().overallMatch(), job.match().javaMatch(), job.match().springMatch(), job.match().backendMatch(), job.match().visaMatch(), masterResume.content(), trainingRulesFor(job));

        var response = modelProvider.generate(new ChatRequest(model, "applicationPackage", prompt, java.util.Map.of("temperature", 0.2, "num_ctx", 4096)));
        if (response.fallback() || response.text() == null || response.text().isBlank()) {
            return new GeneratedApplicationDraft(fallbackResume + "\n\n_AI fallback: " + response.error() + "_", fallbackCoverLetter + "\n\n_AI fallback: " + response.error() + "_", Map.of(), job.match().overallMatch(), job.visa().reason(), 40);
        }
        try {
            GeneratedApplicationDraft draft = parseDraft(response.text(), fallbackResume, fallbackCoverLetter);
            Files.createDirectories(cachePath.getParent());
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(cachePath.toFile(), draft);
            return draft;
        } catch (Exception ex) {
            return new GeneratedApplicationDraft(fallbackResume + "\n\n_AI fallback: Could not parse model JSON._", fallbackCoverLetter, Map.of(), job.match().overallMatch(), job.visa().reason(), 35);
        }
    }

    private GeneratedApplicationDraft parseDraft(String text, String fallbackResume, String fallbackCoverLetter) throws java.io.IOException {
        JsonNode root = objectMapper.readTree(jsonOnly(text));
        String resume = textOr(root, "resume", fallbackResume);
        String coverLetter = textOr(root, "coverLetter", fallbackCoverLetter);
        Map<String, String> answers = root.has("answers")
                ? objectMapper.convertValue(root.get("answers"), new com.fasterxml.jackson.core.type.TypeReference<>() {
                })
                : Map.of();
        return new GeneratedApplicationDraft(
                resume,
                coverLetter,
                answers,
                root.path("matchScore").asInt(0),
                textOr(root, "visaAnalysis", ""),
                root.path("confidence").asInt(50)
        );
    }

    private String fallbackResume(JobRecord job, MasterResume masterResume) {
        String sourceStatus = masterResume.content().equals(MasterResume.empty().content())
                ? "Master resume is not filled in yet. Paste the real master resume before submitting any application."
                : "Tailoring source is the saved Master Resume. Do not add experience outside that source.";
        return """
                # Tailored Resume Draft

                Role: %s
                Company: %s

                %s

                ## Target Signals

                - Overall Match: %d
                - Java Match: %d
                - Spring Match: %d
                - Backend Match: %d
                - Visa Match: %d

                ## Master Resume Source

                %s
                """.formatted(job.title(), job.company(), sourceStatus, job.match().overallMatch(), job.match().javaMatch(), job.match().springMatch(), job.match().backendMatch(), job.match().visaMatch(), masterResume.content());
    }

    private String fallbackCoverLetter(JobRecord job) {
        return """
                # Cover Letter Draft

                Dear Hiring Team,

                I am interested in the %s role at %s. The position aligns with my backend engineering experience, especially around Java, Spring, APIs, and reliable service design.

                Atlas generated this as a draft only. Review company-specific details before submitting.

                Sincerely,
                Sandeep
                """.formatted(job.title(), job.company());
    }

    private String jsonOnly(String value) {
        String text = value == null ? "" : value.trim();
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return text.substring(start, end + 1);
        }
        return text;
    }

    private String textOr(JsonNode node, String field, String fallback) {
        String value = node.path(field).asText("");
        return value.isBlank() ? fallback : value;
    }

    private String cacheKey(JobRecord job, MasterResume masterResume, String model) {
        String rules = repository.answerTrainingRules().stream()
                .filter(AnswerTrainingRule::enabled)
                .map(rule -> rule.id() + ":" + rule.updatedAt())
                .collect(java.util.stream.Collectors.joining("|"));
        return sha256(model + "\n" + job.company() + "\n" + job.title() + "\n" + job.description() + "\n" + masterResume.updatedAt() + "\n" + rules + "\n" + masterResume.content());
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder out = new StringBuilder();
            for (byte b : hash) {
                out.append(String.format("%02x", b));
            }
            return out.toString();
        } catch (Exception ex) {
            return Slug.of(value).substring(0, Math.min(80, Slug.of(value).length()));
        }
    }

    private String trim(String value, int max) {
        if (value == null || value.length() <= max) {
            return value == null ? "" : value;
        }
        return value.substring(0, max);
    }

    private String trainingRulesFor(JobRecord job) {
        String text = (job.title() + "\n" + job.description()).toLowerCase(java.util.Locale.ROOT);
        List<AnswerTrainingRule> rules = repository.answerTrainingRules().stream()
                .filter(AnswerTrainingRule::enabled)
                .filter(rule -> matchesRule(rule, text))
                .limit(8)
                .toList();
        if (rules.isEmpty()) {
            rules = repository.answerTrainingRules().stream()
                    .filter(AnswerTrainingRule::enabled)
                    .limit(5)
                    .toList();
        }
        if (rules.isEmpty()) {
            return "No saved answer training rules yet.";
        }
        StringBuilder out = new StringBuilder();
        for (AnswerTrainingRule rule : rules) {
            out.append("- Pattern: ").append(rule.questionPattern()).append("\n");
            out.append("  Preferred format: ").append(rule.preferredFormat()).append("\n");
            if (rule.exampleAnswer() != null && !rule.exampleAnswer().isBlank()) {
                out.append("  Example answer: ").append(rule.exampleAnswer()).append("\n");
            }
        }
        return out.toString();
    }

    private boolean matchesRule(AnswerTrainingRule rule, String text) {
        if (rule.questionPattern() == null || rule.questionPattern().isBlank()) {
            return true;
        }
        for (String token : rule.questionPattern().toLowerCase(java.util.Locale.ROOT).split("[,\\s]+")) {
            if (token.length() >= 4 && text.contains(token)) {
                return true;
            }
        }
        return false;
    }

    public String reportMarkdown(JobRecord job, ApplicationPackage applicationPackage) {
        return """
                # Application Review Package

                Company: %s
                Role: %s
                Recommendation: %s
                Confidence: %d

                ## Visa

                %s

                ## Match Explanation

                %s

                ## Required Human Review

                - Verify resume tailoring against master resume.
                - Review cover letter for company-specific accuracy.
                - Review all application answers.
                - Approve before Browser Agent can apply.
                """.formatted(job.company(), job.title(), applicationPackage.recommendation(), applicationPackage.recommendationConfidence(), job.visa().reason(), job.match().explanations());
    }

    private String professionalSummary(JobRecord job) {
        return "I am a backend-focused software engineer with experience aligned to " + job.title() + ". I focus on Java, Spring, APIs, system reliability, and practical delivery. This answer must be reviewed against the master resume before submission.";
    }

    private String authorizationAnswer(JobRecord job) {
        if (job.visa().score() < 40) {
            return "This role appears to contain sponsorship restrictions. Review manually before answering work authorization questions.";
        }
        return "I can discuss work authorization requirements during the process. Review this answer manually before submission.";
    }

    private boolean containsIgnoreCase(List<String> values, String candidate) {
        if (values == null || candidate == null) {
            return false;
        }
        return values.stream().anyMatch(value -> candidate.equalsIgnoreCase(value));
    }

    private record GeneratedApplicationDraft(String resume, String coverLetter, Map<String, String> answers, int matchScore, String visaAnalysis, int confidence) {
        String answer(String key, String fallback) {
            String value = answers == null ? "" : answers.getOrDefault(key, "");
            return value == null || value.isBlank() ? fallback : value;
        }
    }
}
