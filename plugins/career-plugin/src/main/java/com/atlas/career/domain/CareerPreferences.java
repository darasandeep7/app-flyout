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
                List.of(
                        "Senior Backend Engineer",
                        "Senior Java Engineer",
                        "Lead Java Engineer",
                        "Backend Software Engineer",
                        "Senior Software Engineer Backend",
                        "Java Backend Developer",
                        "Platform Engineer",
                        "Cloud Backend Engineer",
                        "Distributed Systems Engineer",
                        "Staff Backend Engineer"
                ),
                List.of(
                        "Java 21",
                        "Java",
                        "Spring Boot",
                        "Microservices",
                        "REST APIs",
                        "Kafka",
                        "Event-Driven Architecture",
                        "Snowflake",
                        "Kubernetes",
                        "Azure",
                        "Redis",
                        "Python",
                        "Distributed Systems",
                        "High Availability",
                        "Fault Tolerance",
                        "Batch Processing",
                        "Hibernate",
                        "API Gateway",
                        "Apigee",
                        "SQL",
                        "Snowpark"
                ),
                List.of("Remote", "Dallas", "Coppell", "Irving", "Plano", "Texas"),
                "Remote preferred",
                "Hybrid acceptable",
                0,
                true,
                70,
                List.of(),
                List.of(),
                "08:00",
                10
        );
    }
}
