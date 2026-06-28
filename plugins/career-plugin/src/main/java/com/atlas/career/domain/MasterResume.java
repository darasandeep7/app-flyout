package com.atlas.career.domain;

import java.time.Instant;
import java.util.List;

public record MasterResume(
        String content,
        List<String> preferredSkills,
        List<String> preferredKeywords,
        List<String> versions,
        Instant updatedAt
) {
    public static MasterResume empty() {
        return new MasterResume(
                "# Master Resume\n\nPaste your real master resume here. Atlas will never invent experience; it will only reorder, rephrase, highlight, and tailor what exists in this source.",
                List.of("Java", "Spring Boot", "Microservices", "Backend APIs", "Snowflake"),
                List.of("Java", "Spring", "Backend", "Microservices", "Cloud", "Leadership"),
                List.of(),
                Instant.EPOCH
        );
    }
}
