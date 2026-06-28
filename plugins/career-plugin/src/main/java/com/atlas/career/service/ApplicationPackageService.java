package com.atlas.career.service;

import com.atlas.ai.ChatRequest;
import com.atlas.ai.ModelProvider;
import com.atlas.career.domain.ApplicationPackage;
import com.atlas.career.domain.ApplicationQuestionAnswer;
import com.atlas.career.domain.CareerPreferences;
import com.atlas.career.domain.JobRecord;
import com.atlas.career.domain.MasterResume;
import com.atlas.common.Slug;
import com.atlas.recommendation.RecommendationCategory;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ApplicationPackageService {
    private final CareerRepository repository;
    private final ModelProvider modelProvider;

    public ApplicationPackageService(CareerRepository repository, ModelProvider modelProvider) {
        this.repository = repository;
        this.modelProvider = modelProvider;
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
        List<ApplicationQuestionAnswer> answers = List.of(
                new ApplicationQuestionAnswer("Tell me about yourself.", answer(job, masterResume, "Tell me about yourself.", professionalSummary(job)), "Generated from the Master Resume and job description.", true),
                new ApplicationQuestionAnswer("Why this company?", answer(job, masterResume, "Why this company?", "I am interested in " + job.company() + " because the role aligns with my backend engineering strengths and the company's hiring needs. I would review company-specific details before submitting."), "Draft requiring company research review.", true),
                new ApplicationQuestionAnswer("Work authorization", authorizationAnswer(job), "Generated from visa intelligence.", true),
                new ApplicationQuestionAnswer("Salary expectations", answer(job, masterResume, "Salary expectations", "Open to a competitive market-aligned package based on scope, level, and total compensation."), "Reusable saved answer.", true)
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
                folder + "/resume.md",
                folder + "/cover-letter.md",
                folder + "/answers.json",
                folder + "/application-report.md",
                answers,
                now,
                now
        );
    }

    public String resumeMarkdown(JobRecord job) {
        MasterResume masterResume = repository.masterResume();
        String sourceStatus = masterResume.content().equals(MasterResume.empty().content())
                ? "Master resume is not filled in yet. Paste the real master resume before submitting any application."
                : "Tailoring source is the saved Master Resume. Do not add experience outside that source.";
        String fallback = """
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
        if (masterResume.content().equals(MasterResume.empty().content())) {
            return fallback;
        }
        return generate("""
                You are tailoring Sandeep's resume for a job application.
                Use ONLY facts from the Master Resume.
                Never invent employers, titles, tools, metrics, dates, projects, education, certifications, or experience.
                You may reorder, rephrase, highlight, and optimize for ATS.
                Return a complete ATS-friendly resume in Markdown.

                Job:
                Company: %s
                Title: %s
                Location: %s
                Description:
                %s

                Master Resume:
                %s
                """.formatted(job.company(), job.title(), job.location(), job.description(), masterResume.content()), fallback);
    }

    public String coverLetterMarkdown(JobRecord job) {
        MasterResume masterResume = repository.masterResume();
        String fallback = """
                # Cover Letter Draft

                Dear Hiring Team,

                I am interested in the %s role at %s. The position aligns with my backend engineering experience, especially around Java, Spring, APIs, and reliable service design.

                Atlas generated this as a draft only. Review company-specific details before submitting.

                Sincerely,
                Sandeep
                """.formatted(job.title(), job.company());
        if (masterResume.content().equals(MasterResume.empty().content())) {
            return fallback;
        }
        return generate("""
                Write a concise cover letter for Sandeep.
                Use ONLY facts from the Master Resume and job description.
                Do not invent company facts, career history, metrics, or experience.
                Keep it professional, specific to the role, and under 300 words.
                Return Markdown.

                Company: %s
                Role: %s
                Job Description:
                %s

                Master Resume:
                %s
                """.formatted(job.company(), job.title(), job.description(), masterResume.content()), fallback);
    }

    private String answer(JobRecord job, MasterResume masterResume, String question, String fallback) {
        if (masterResume.content().equals(MasterResume.empty().content())) {
            return fallback;
        }
        return generate("""
                Draft an application answer for Sandeep.
                Use ONLY facts from the Master Resume and job description.
                Never invent experience.
                Keep the answer direct and editable.

                Question: %s
                Company: %s
                Role: %s
                Job Description:
                %s

                Master Resume:
                %s
                """.formatted(question, job.company(), job.title(), job.description(), masterResume.content()), fallback);
    }

    private String generate(String prompt, String fallback) {
        var response = modelProvider.generate(new ChatRequest(null, prompt, java.util.Map.of("temperature", 0.2)));
        if (response.fallback() || response.text() == null || response.text().isBlank()) {
            return fallback + "\n\n_AI fallback: " + (response.error() == null ? "Ollama unavailable." : response.error()) + "_";
        }
        return response.text().trim();
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
}
