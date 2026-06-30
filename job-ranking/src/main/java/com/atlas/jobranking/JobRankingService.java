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
        int role = roleMatch(request.title(), text);
        int java = concept(text, 96, 70, "java", "jvm", "core java", "java 8", "java 11", "java 17", "java 21");
        int spring = concept(text, 94, 78, "spring boot", "spring", "spring mvc", "spring framework", "spring cloud", "spring data");
        int snowflake = concept(text, 88, 72, "snowflake", "data warehouse", "modern data warehouse", "redshift", "bigquery", "databricks");
        int backend = concept(text, 94, 76, "backend", "back end", "server side", "api", "rest api", "restful api", "integration", "platform engineering");
        int microservices = concept(text, 92, 74, "microservice", "microservices", "distributed system", "distributed systems", "event driven", "service oriented");
        int cloud = concept(text, 86, 72, "aws", "azure", "gcp", "cloud", "kubernetes", "container", "docker");
        int messaging = concept(text, 86, 72, "kafka", "rabbitmq", "messaging", "event streaming", "pub sub", "queue");
        int technical = average(java, spring, backend, microservices, cloud, messaging, snowflake);
        int experience = experienceMatch(text);
        int resumeStrength = resumeStrength(text);
        int salary = request.salary() == null || request.salary().isBlank() ? 78 : 88;
        int location = locationScore(request.location());
        int remote = remoteScore(request.remoteStatus(), request.location());
        int visa = visaScore(request);
        boolean relevant = role >= 70 || technical >= 70 || java >= 80 || backend >= 80 || spring >= 78;

        int overall = (role * 35 + technical * 30 + experience * 15 + location * 5 + salary * 5 + visa * 5 + resumeStrength * 5) / 100;
        if (!relevant) {
            overall = Math.min(overall, 39);
            technical = Math.min(technical, 45);
        }
        overall = calibrate(overall, role, technical, experience, text, relevant);
        int growth = resumeStrength;
        int interview = Math.max(10, Math.min(96, (overall * 2 + technical + experience + visa) / 5));
        int confidence = Math.max(45, Math.min(96, 52 + signalCount(text) * 6));

        Map<String, String> explanations = new LinkedHashMap<>();
        explanations.put("Overall", "Weighted score: Role 35%, Technical 30%, Experience 15%, Location 5%, Salary 5%, Visa 5%, Resume Strength 5%.");
        explanations.put("Role Match", role >= 85 ? "Strong Java/backend/platform role match." : relevant ? "Related role with useful target-field signals." : "Not enough Java/backend role signal.");
        explanations.put("Technical", "Equivalent technologies receive partial credit: Spring MVC to Spring Boot, RESTful APIs to REST APIs, RabbitMQ to Kafka/messaging, Azure/AWS/GCP cloud equivalents, and Snowflake to data warehouse.");
        explanations.put("Experience", experience >= 85 ? "Required seniority fits; extra experience is not penalized." : "Seniority requires review.");
        explanations.put("Resume Strength", "Leadership, architecture, scalability, platform, integration, and cloud signals.");
        explanations.put("Visa", request.visa() == null ? "Sponsorship Unknown." : request.visa().reason());
        explanations.put("Confidence", "Confidence rises with explicit role, technical, seniority, and platform signals.");

        return new JobIntelligenceScore(overall, technical, java, spring, snowflake, backend, microservices, cloud, resumeStrength, salary, location, remote, visa, growth, interview, confidence, explanations);
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

    private int roleMatch(String title, String text) {
        String normalizedTitle = normalize(title);
        int score = 38;
        score = Math.max(score, phraseScore(normalizedTitle, 96, "senior java backend engineer", "senior backend engineer", "senior java engineer"));
        score = Math.max(score, phraseScore(normalizedTitle, 94, "backend software engineer", "software engineer backend", "backend api engineer"));
        score = Math.max(score, phraseScore(normalizedTitle, 90, "platform engineer", "cloud platform engineer", "data platform engineer", "integration engineer"));
        score = Math.max(score, phraseScore(normalizedTitle, 88, "staff java engineer", "staff backend engineer", "lead java engineer"));
        score = Math.max(score, phraseScore(normalizedTitle, 82, "software engineer", "application developer", "java developer"));
        if (containsAny(text, "java", "spring", "backend", "rest api", "microservice", "platform engineering")) score = Math.max(score, 78);
        if (containsAny(normalizedTitle, "frontend", "mobile", "ios", "android", "qa", "sales", "product manager", "designer")) score = Math.min(score, 45);
        return score;
    }

    private int concept(String text, int directScore, int equivalentScore, String... phrases) {
        for (String phrase : phrases) {
            if (text.contains(normalize(phrase))) return directScore;
        }
        return equivalentSignal(text, phrases) ? equivalentScore : 45;
    }

    private boolean equivalentSignal(String text, String[] phrases) {
        String joined = String.join(" ", phrases);
        if (joined.contains("spring") && containsAny(text, "spring mvc", "spring framework", "spring cloud")) return true;
        if (joined.contains("rest") && containsAny(text, "restful", "web service", "http api", "api design")) return true;
        if (joined.contains("kafka") && containsAny(text, "rabbitmq", "messaging", "queue", "event streaming")) return true;
        if (joined.contains("aws") && containsAny(text, "azure", "gcp", "cloud platform")) return true;
        if (joined.contains("snowflake") && containsAny(text, "data warehouse", "redshift", "bigquery", "databricks")) return true;
        return false;
    }

    private int experienceMatch(String text) {
        int years = requiredYears(text);
        if (years == 0) return containsAny(text, "senior", "lead", "staff") ? 88 : 82;
        if (years <= 12) return 94;
        if (years <= 15) return 88;
        return 78;
    }

    private int requiredYears(String text) {
        int max = 0;
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("(\\d{1,2})\\+?\\s*(?:years|yrs)").matcher(text);
        while (matcher.find()) max = Math.max(max, Integer.parseInt(matcher.group(1)));
        return max;
    }

    private int resumeStrength(String text) {
        int score = 64;
        if (containsAny(text, "lead", "mentor", "architecture", "architect", "design")) score += 10;
        if (containsAny(text, "scalable", "scale", "high availability", "resilient", "distributed")) score += 10;
        if (containsAny(text, "cloud", "aws", "azure", "gcp", "kubernetes")) score += 8;
        if (containsAny(text, "platform", "integration", "api gateway", "data platform")) score += 8;
        return clamp(score);
    }

    private int locationScore(String location) {
        String normalized = normalize(location);
        if (normalized.isBlank()) return 72;
        if (containsAny(normalized, "remote", "dallas", "coppell", "irving", "plano", "texas", "california", "san francisco", "san jose", "sunnyvale")) return 94;
        if (settings.preferredLocations() != null && settings.preferredLocations().stream().anyMatch(value -> normalized.contains(normalize(value)))) return 90;
        if (containsAny(normalized, "hybrid")) return 78;
        return 62;
    }

    private int remoteScore(String remoteStatus, String location) {
        String combined = normalize(remoteStatus + " " + location);
        if (combined.contains("remote")) return 90;
        if (combined.contains("hybrid")) return 74;
        return 58;
    }

    private int visaScore(JobIntelligenceRequest request) {
        if (request.visa() == null) return 65;
        String reason = normalize(request.visa().reason());
        if (request.visa().visaScore() >= 75 || containsAny(reason, "sponsorship available", "will sponsor", "sponsor h1b")) return 92;
        if (request.visa().visaScore() <= 25 || containsAny(reason, "no sponsorship", "unable to sponsor", "not sponsor", "unrestricted work authorization")) return 20;
        return 68;
    }

    private int calibrate(int overall, int role, int technical, int experience, String text, boolean relevant) {
        if (!relevant) return Math.min(overall, 39);
        if (containsAny(text, "senior") && role >= 85 && technical >= 82) return Math.max(overall, 87);
        if (containsAny(text, "staff") && role >= 80 && technical >= 78) return Math.max(overall, 82);
        if (containsAny(text, "platform engineer", "platform engineering") && technical >= 76) return Math.max(overall, 80);
        if (containsAny(text, "data engineer", "data platform") && technical >= 72 && containsAny(text, "java")) return Math.max(overall, 76);
        if (role >= 80 && technical >= 80 && experience >= 80) return Math.max(overall, 84);
        return overall;
    }

    private int phraseScore(String text, int score, String... phrases) {
        for (String phrase : phrases) {
            if (text.contains(phrase)) return score;
        }
        return 0;
    }

    private int signalCount(String text) {
        int count = 0;
        for (String signal : List.of("java", "spring", "backend", "microservice", "api", "platform", "integration", "cloud", "kafka", "snowflake")) {
            if (text.contains(signal)) count++;
        }
        return count;
    }

    private boolean containsAny(String text, String... values) {
        for (String value : values) {
            if (text.contains(normalize(value))) return true;
        }
        return false;
    }

    private int average(int... values) {
        int total = 0;
        for (int value : values) total += value;
        return values.length == 0 ? 0 : total / values.length;
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

    private int clamp(int value) {
        return Math.max(0, Math.min(100, value));
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9+#.]+", " ").replaceAll("\\s+", " ").trim();
    }
}
