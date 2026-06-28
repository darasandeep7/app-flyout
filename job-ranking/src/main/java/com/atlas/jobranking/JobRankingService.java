package com.atlas.jobranking;

import com.atlas.settings.CareerSettings;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class JobRankingService {
    private final CareerSettings settings;

    public JobRankingService(CareerSettings settings) {
        this.settings = settings;
    }

    public JobIntelligenceScore score(JobIntelligenceRequest request) {
        String text = normalize(request.title() + "\n" + request.description());
        int java = contains(text, "java", "jvm") ? 92 : 35;
        int spring = contains(text, "spring", "spring boot") ? 90 : 35;
        int snowflake = contains(text, "snowflake") ? 86 : 45;
        int backend = contains(text, "backend", "api", "microservice", "distributed") ? 88 : 50;
        int microservices = contains(text, "microservice", "distributed", "event-driven") ? 86 : 45;
        int cloud = contains(text, "aws", "azure", "gcp", "cloud", "kubernetes") ? 78 : 50;
        int leadership = contains(text, "lead", "principal", "architect", "mentor", "staff") ? 84 : 55;
        int salary = request.salary() == null || request.salary().isBlank() ? 60 : 75;
        int location = locationScore(request.location());
        int remote = remoteScore(request.remoteStatus(), request.location());
        int visa = request.visa() == null ? 55 : request.visa().visaScore();
        int technical = (java + spring + snowflake + backend + microservices + cloud) / 6;
        int growth = contains(text, "senior", "staff", "principal", "architect", "lead") ? 82 : 65;
        int overall = (technical * 26 + leadership * 10 + salary * 8 + location * 8 + remote * 8 + visa * 16 + growth * 12 + java * 6 + spring * 6) / 100;
        int interview = Math.max(10, Math.min(95, (overall + technical + visa + leadership) / 4));
        int confidence = Math.max(35, Math.min(95, 45 + signalCount(text) * 7));

        Map<String, String> explanations = new LinkedHashMap<>();
        explanations.put("Overall", "Weighted blend of technical fit, visa fit, location, remote preference, salary, and growth.");
        explanations.put("Technical", "Computed from Java, Spring, Snowflake, backend, microservices, and cloud signals.");
        explanations.put("Visa", request.visa() == null ? "Visa analysis unavailable." : request.visa().reason());
        explanations.put("Career Growth", growth >= 80 ? "Role appears aligned with senior growth." : "Growth signal is moderate.");
        explanations.put("Confidence", "Confidence rises with explicit signals in the job description.");

        return new JobIntelligenceScore(overall, technical, java, spring, snowflake, backend, microservices, cloud, leadership, salary, location, remote, visa, growth, interview, confidence, explanations);
    }

    public DuplicateAssessment duplicate(JobIntelligenceRequest left, JobIntelligenceRequest right) {
        List<String> signals = new ArrayList<>();
        if (normalize(left.company()).equals(normalize(right.company()))) signals.add("company");
        if (normalizeTitle(left.title()).equals(normalizeTitle(right.title()))) signals.add("normalized title");
        if (normalize(left.location()).equals(normalize(right.location()))) signals.add("location");
        if (normalize(left.salary()).equals(normalize(right.salary()))) signals.add("salary");
        if (normalize(left.remoteStatus()).equals(normalize(right.remoteStatus()))) signals.add("remote status");
        if (left.postingDate() != null && right.postingDate() != null && Math.abs(Duration.between(left.postingDate(), right.postingDate()).toDays()) <= 7) signals.add("posting date");
        if (descriptionSimilarity(left.description(), right.description()) >= 70) signals.add("description similarity");
        if (normalize(left.experienceLevel()).equals(normalize(right.experienceLevel()))) signals.add("experience level");
        int confidence = Math.min(98, signals.size() * 13);
        return new DuplicateAssessment(confidence >= 65, confidence, signals);
    }

    private int locationScore(String location) {
        String normalized = normalize(location);
        if (settings.preferredLocations() != null && settings.preferredLocations().stream().anyMatch(value -> normalized.contains(normalize(value)))) {
            return 90;
        }
        return normalized.contains("remote") ? 85 : 55;
    }

    private int remoteScore(String remoteStatus, String location) {
        String combined = normalize(remoteStatus + " " + location);
        if (combined.contains("remote")) return 90;
        if (combined.contains("hybrid")) return 70;
        return 55;
    }

    private int signalCount(String text) {
        int count = 0;
        for (String signal : List.of("java", "spring", "snowflake", "backend", "microservice", "cloud", "lead", "api")) {
            if (text.contains(signal)) count++;
        }
        return count;
    }

    private int descriptionSimilarity(String left, String right) {
        String l = normalize(left);
        String r = normalize(right);
        if (l.isBlank() || r.isBlank()) return 0;
        int overlap = 0;
        for (String token : l.split(" ")) {
            if (token.length() > 4 && r.contains(token)) overlap++;
        }
        return Math.min(100, overlap * 100 / Math.max(1, l.split(" ").length));
    }

    private String normalizeTitle(String value) {
        return normalize(value).replace("senior", "sr").replaceAll("[^a-z0-9]+", " ").trim();
    }

    private boolean contains(String text, String... values) {
        for (String value : values) {
            if (text.contains(value)) return true;
        }
        return false;
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
    }
}
