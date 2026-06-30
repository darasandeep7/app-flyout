package com.atlas.career.service;

import com.atlas.career.domain.CareerPreferences;
import com.atlas.career.domain.MatchAssessment;
import com.atlas.career.domain.VisaAssessment;
import com.atlas.settings.CareerSettings;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class MatchEngine {
    private final CareerSettings settings;
    private final CareerRepository repository;

    public MatchEngine(CareerSettings settings, CareerRepository repository) {
        this.settings = settings;
        this.repository = repository;
    }

    public MatchAssessment score(String title, String location, String description, String salary, VisaAssessment visa) {
        CareerPreferences preferences = repository.preferences();
        String text = normalize((title == null ? "" : title) + "\n" + (description == null ? "" : description));

        int role = roleMatch(title, text);
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
        int salaryMatch = salaryScore(salary, preferences.minimumSalary());
        int locationMatch = locationScore(location, preferences);
        int visaScore = visaScore(visa);

        boolean relevant = role >= 70 || technical >= 70 || java >= 80 || backend >= 80 || spring >= 78;
        int overall = (role * 35
                + technical * 30
                + experience * 15
                + locationMatch * 5
                + salaryMatch * 5
                + visaScore * 5
                + resumeStrength * 5) / 100;
        if (!relevant) {
            overall = Math.min(overall, 39);
            technical = Math.min(technical, 45);
        }
        overall = calibrate(overall, role, technical, experience, text, relevant);
        int resume = average(role, technical, experience, resumeStrength);
        int leadership = resumeStrength;
        int interview = Math.max(12, Math.min(96, (overall * 2 + technical + experience + visaScore) / 5));

        Map<String, String> explanations = new LinkedHashMap<>();
        explanations.put("Overall", "Weighted score: Role 35%, Technical 30%, Experience 15%, Location 5%, Salary 5%, Visa 5%, Resume Strength 5%.");
        explanations.put("Role Match", role >= 85 ? "Strong Java/backend/platform role signal." : relevant ? "Related role with some target-field signal." : "Role is outside the Java/backend target field.");
        explanations.put("Technical Match", "Uses equivalent technology groups, so Spring MVC counts toward Spring Boot, RESTful APIs toward REST APIs, RabbitMQ toward messaging, and Azure/AWS/GCP as partial cloud equivalents.");
        explanations.put("Experience Match", experience >= 85 ? "Required seniority fits your experience; extra experience is not penalized." : "Seniority or required years need review.");
        explanations.put("Resume Strength", resumeStrength >= 85 ? "Leadership, architecture, scalability, and cloud/platform signals are strong." : "Resume-strength signals are moderate for this posting.");
        explanations.put("Salary", salaryMatch >= 80 ? "Salary meets or does not conflict with preferences." : "Salary is missing, below preference, or needs review.");
        explanations.put("Location", locationMatch >= 85 ? "Location aligns with Dallas, Remote, California, or configured preferences." : "Location is less aligned.");
        explanations.put("Visa", visaExplanation(visa));

        return new MatchAssessment(overall, resume, java, spring, snowflake, backend, leadership, salaryMatch, locationMatch, visaScore, interview, explanations);
    }

    private int roleMatch(String title, String text) {
        String normalizedTitle = normalize(title);
        int score = 38;
        score = Math.max(score, phraseScore(normalizedTitle, 96, "senior java backend engineer", "senior backend engineer", "senior java engineer"));
        score = Math.max(score, phraseScore(normalizedTitle, 94, "backend software engineer", "software engineer backend", "backend api engineer"));
        score = Math.max(score, phraseScore(normalizedTitle, 90, "platform engineer", "cloud platform engineer", "data platform engineer", "integration engineer"));
        score = Math.max(score, phraseScore(normalizedTitle, 88, "staff java engineer", "staff backend engineer", "lead java engineer"));
        score = Math.max(score, phraseScore(normalizedTitle, 82, "software engineer", "application developer", "java developer"));
        if (containsAny(text, "java", "spring", "backend", "rest api", "microservice", "platform engineering")) {
            score = Math.max(score, 78);
        }
        if (containsAny(normalizedTitle, "frontend", "mobile", "ios", "android", "qa", "sales", "product manager", "designer")) {
            score = Math.min(score, 45);
        }
        return score;
    }

    private int concept(String text, int directScore, int equivalentScore, String... phrases) {
        for (String phrase : phrases) {
            if (containsPhrase(text, phrase)) {
                return directScore;
            }
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
        while (matcher.find()) {
            max = Math.max(max, Integer.parseInt(matcher.group(1)));
        }
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

    private int locationScore(String location, CareerPreferences preferences) {
        String normalized = normalize(location);
        if (normalized.isBlank()) return 72;
        if (containsAny(normalized, "remote", "dallas", "coppell", "irving", "plano", "texas", "california", "san francisco", "san jose", "sunnyvale")) return 94;
        if (preferences.preferredLocations() != null && preferences.preferredLocations().stream().anyMatch(value -> normalized.contains(normalize(value)))) return 92;
        if (settings.preferredLocations() != null && settings.preferredLocations().stream().anyMatch(value -> normalized.contains(normalize(value)))) return 90;
        if (containsAny(normalized, "hybrid")) return 78;
        return 62;
    }

    private int salaryScore(String salary, int minimumSalary) {
        if (minimumSalary <= 0 || salary == null || salary.isBlank()) return 78;
        int highest = 0;
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("\\d{2,6}").matcher(salary.replace(",", ""));
        while (matcher.find()) {
            int value = Integer.parseInt(matcher.group());
            highest = Math.max(highest, value < 1000 ? value * 1000 : value);
        }
        if (highest == 0) return 70;
        return highest >= minimumSalary ? 92 : highest >= minimumSalary * 0.9 ? 78 : 48;
    }

    private int visaScore(VisaAssessment visa) {
        if (visa == null) return 65;
        String reason = normalize(visa.reason());
        if (visa.score() >= 75 || containsAny(reason, "sponsorship available", "will sponsor", "sponsor h1b")) return 92;
        if (visa.score() <= 25 || containsAny(reason, "no sponsorship", "unable to sponsor", "not sponsor", "unrestricted work authorization")) return 20;
        return 68;
    }

    private String visaExplanation(VisaAssessment visa) {
        if (visa == null) return "Sponsorship Unknown.";
        int score = visaScore(visa);
        if (score >= 85) return "Sponsorship Available: " + visa.reason();
        if (score <= 30) return "Sponsorship Not Available: " + visa.reason();
        return "Sponsorship Unknown: " + visa.reason();
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

    private int average(int... values) {
        int total = 0;
        for (int value : values) total += value;
        return values.length == 0 ? 0 : total / values.length;
    }

    private boolean containsPhrase(String text, String value) {
        return text.contains(normalize(value));
    }

    private boolean containsAny(String text, String... values) {
        for (String value : values) {
            if (text.contains(normalize(value))) return true;
        }
        return false;
    }

    private int clamp(int value) {
        return Math.max(0, Math.min(100, value));
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9+#.]+", " ").replaceAll("\\s+", " ").trim();
    }
}
