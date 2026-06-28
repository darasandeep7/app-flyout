package com.atlas.career.service;

import com.atlas.career.domain.MatchAssessment;
import com.atlas.career.domain.VisaAssessment;
import com.atlas.settings.CareerSettings;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class MatchEngine {
    private final CareerSettings settings;

    public MatchEngine(CareerSettings settings) {
        this.settings = settings;
    }

    public MatchAssessment score(String title, String location, String description, String salary, VisaAssessment visa) {
        String text = ((title == null ? "" : title) + "\n" + (description == null ? "" : description)).toLowerCase(Locale.ROOT);
        int java = containsAny(text, "java", "jvm") ? 90 : 35;
        int spring = containsAny(text, "spring", "spring boot") ? 90 : 35;
        int snowflake = containsAny(text, "snowflake") ? 85 : 45;
        int backend = containsAny(text, "backend", "api", "microservice", "distributed") ? 88 : 50;
        int leadership = containsAny(text, "lead", "principal", "architect", "mentor", "staff") ? 85 : 55;
        int locationMatch = locationScore(location);
        int salaryMatch = salary == null || salary.isBlank() ? 60 : 75;
        int resume = (java + spring + backend + leadership) / 4;
        int visaScore = visa == null ? 50 : visa.score();
        int overall = weighted(resume, java, spring, snowflake, backend, leadership, salaryMatch, locationMatch, visaScore);
        int interview = Math.max(15, Math.min(92, (overall + resume + visaScore) / 3));

        Map<String, String> explanations = new LinkedHashMap<>();
        explanations.put("Java", java >= 80 ? "Strong Java signal detected." : "Java signal is weak or missing.");
        explanations.put("Spring", spring >= 80 ? "Spring/Spring Boot appears in the role." : "Spring is not prominent.");
        explanations.put("Snowflake", snowflake >= 80 ? "Snowflake appears in the role." : "Snowflake is not prominent.");
        explanations.put("Backend", backend >= 80 ? "Backend/API/distributed systems language found." : "Backend scope needs manual review.");
        explanations.put("Leadership", leadership >= 80 ? "Leadership or senior ownership language found." : "Leadership signal is moderate.");
        explanations.put("Visa", visa == null ? "Visa assessment unavailable." : visa.reason());

        return new MatchAssessment(overall, resume, java, spring, snowflake, backend, leadership, salaryMatch, locationMatch, visaScore, interview, explanations);
    }

    private int locationScore(String location) {
        if (location == null || location.isBlank()) {
            return 60;
        }
        String normalized = location.toLowerCase(Locale.ROOT);
        if (settings.preferredLocations() != null && settings.preferredLocations().stream().anyMatch(value -> normalized.contains(value.toLowerCase(Locale.ROOT)))) {
            return 90;
        }
        return normalized.contains("remote") ? 85 : 55;
    }

    private boolean containsAny(String text, String... values) {
        for (String value : values) {
            if (text.contains(value)) {
                return true;
            }
        }
        return false;
    }

    private int weighted(int resume, int java, int spring, int snowflake, int backend, int leadership, int salary, int location, int visa) {
        return (resume * 20 + java * 12 + spring * 12 + snowflake * 8 + backend * 14 + leadership * 10 + salary * 6 + location * 8 + visa * 10) / 100;
    }
}
