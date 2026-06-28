package com.atlas.career.service;

import com.atlas.career.domain.MatchAssessment;
import com.atlas.career.domain.VisaAssessment;
import com.atlas.career.domain.CareerPreferences;
import com.atlas.settings.CareerSettings;
import java.util.List;
import java.util.LinkedHashMap;
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
        String text = ((title == null ? "" : title) + "\n" + (description == null ? "" : description)).toLowerCase(Locale.ROOT);
        int titleFit = preferredTitleScore(title, preferences.preferredTitles());
        int java = containsAny(text, "java", "jvm") || containsPreferredSkill(text, preferences.preferredSkills(), "java") ? 90 : 35;
        int spring = containsAny(text, "spring", "spring boot") || containsPreferredSkill(text, preferences.preferredSkills(), "spring") ? 90 : 35;
        int snowflake = containsAny(text, "snowflake") ? 85 : 45;
        int backend = containsAny(text, "backend", "api", "microservice", "distributed") || preferredSkillOverlap(text, preferences.preferredSkills()) >= 3 ? 88 : 50;
        int leadership = containsAny(text, "lead", "principal", "architect", "mentor", "staff") ? 85 : 55;
        int locationMatch = locationScore(location, preferences);
        int salaryMatch = salaryScore(salary, preferences.minimumSalary());
        int resume = (titleFit + java + spring + backend + leadership) / 5;
        int visaScore = visa == null ? 50 : visa.score();
        int overall = weighted(resume, java, spring, snowflake, backend, leadership, salaryMatch, locationMatch, visaScore);
        int interview = Math.max(15, Math.min(92, (overall + resume + visaScore) / 3));

        Map<String, String> explanations = new LinkedHashMap<>();
        explanations.put("Title", titleFit >= 80 ? "Role title matches preferred titles." : "Role title is outside the strongest preferred title patterns.");
        explanations.put("Java", java >= 80 ? "Strong Java signal detected." : "Java signal is weak or missing.");
        explanations.put("Spring", spring >= 80 ? "Spring/Spring Boot appears in the role." : "Spring is not prominent.");
        explanations.put("Snowflake", snowflake >= 80 ? "Snowflake appears in the role." : "Snowflake is not prominent.");
        explanations.put("Backend", backend >= 80 ? "Backend/API/distributed systems language found." : "Backend scope needs manual review.");
        explanations.put("Leadership", leadership >= 80 ? "Leadership or senior ownership language found." : "Leadership signal is moderate.");
        explanations.put("Salary", salaryMatch >= 75 ? "Salary signal meets or does not conflict with preferences." : "Salary appears below preference or needs review.");
        explanations.put("Location", locationMatch >= 80 ? "Location or remote policy matches preferences." : "Location needs review against preferences.");
        explanations.put("Visa", visa == null ? "Visa assessment unavailable." : visa.reason());

        return new MatchAssessment(overall, resume, java, spring, snowflake, backend, leadership, salaryMatch, locationMatch, visaScore, interview, explanations);
    }

    private int locationScore(String location, CareerPreferences preferences) {
        if (location == null || location.isBlank()) {
            return 60;
        }
        String normalized = location.toLowerCase(Locale.ROOT);
        if (preferences.preferredLocations() != null && preferences.preferredLocations().stream().anyMatch(value -> normalized.contains(value.toLowerCase(Locale.ROOT)))) {
            return 92;
        }
        if (settings.preferredLocations() != null && settings.preferredLocations().stream().anyMatch(value -> normalized.contains(value.toLowerCase(Locale.ROOT)))) {
            return 90;
        }
        return normalized.contains("remote") ? 85 : 55;
    }

    private int preferredTitleScore(String title, List<String> preferredTitles) {
        if (title == null || title.isBlank() || preferredTitles == null || preferredTitles.isEmpty()) {
            return 65;
        }
        String normalized = title.toLowerCase(Locale.ROOT);
        return preferredTitles.stream().anyMatch(value -> normalized.contains(value.toLowerCase(Locale.ROOT))) ? 92 : 65;
    }

    private int salaryScore(String salary, int minimumSalary) {
        if (minimumSalary <= 0 || salary == null || salary.isBlank()) {
            return 70;
        }
        String digits = salary.replaceAll("[^0-9]", " ").trim();
        if (digits.isBlank()) {
            return 60;
        }
        int highest = 0;
        for (String part : digits.split("\\s+")) {
            if (!part.isBlank()) {
                int value = Integer.parseInt(part);
                highest = Math.max(highest, value < 1000 ? value * 1000 : value);
            }
        }
        return highest >= minimumSalary ? 85 : 45;
    }

    private boolean containsAny(String text, String... values) {
        for (String value : values) {
            if (text.contains(value)) {
                return true;
            }
        }
        return false;
    }

    private boolean containsPreferredSkill(String text, List<String> skills, String required) {
        return skills != null && skills.stream()
                .map(value -> value.toLowerCase(Locale.ROOT))
                .anyMatch(value -> value.contains(required) && text.contains(required));
    }

    private int preferredSkillOverlap(String text, List<String> skills) {
        if (skills == null) {
            return 0;
        }
        int count = 0;
        for (String skill : skills) {
            if (skill != null && !skill.isBlank() && text.contains(skill.toLowerCase(Locale.ROOT))) {
                count++;
            }
        }
        return count;
    }

    private int weighted(int resume, int java, int spring, int snowflake, int backend, int leadership, int salary, int location, int visa) {
        return (resume * 20 + java * 12 + spring * 12 + snowflake * 8 + backend * 14 + leadership * 10 + salary * 6 + location * 8 + visa * 10) / 100;
    }
}
