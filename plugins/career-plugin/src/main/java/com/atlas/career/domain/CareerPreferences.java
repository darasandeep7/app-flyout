package com.atlas.career.domain;

import java.util.List;

public record CareerPreferences(
        List<String> preferredTitles,
        List<String> preferredSkills,
        List<String> preferredLocations,
        String remotePreference,
        String hybridPreference,
        int minimumSalary,
        boolean visaRequired,
        int minimumMatchScore,
        List<String> blacklistCompanies,
        List<String> whitelistCompanies,
        String dailyScanTime,
        int maximumApplicationsPerDay
) {
    public static CareerPreferences defaults() {
        return new CareerPreferences(
                List.of("Senior Java Developer", "Senior Backend Engineer", "Lead Java Engineer"),
                List.of("Java", "Spring Boot", "Microservices", "Snowflake", "Backend APIs"),
                List.of("Remote", "Dallas", "Irving", "Plano"),
                "Remote preferred",
                "Hybrid acceptable",
                0,
                true,
                75,
                List.of(),
                List.of(),
                "08:00",
                5
        );
    }
}
