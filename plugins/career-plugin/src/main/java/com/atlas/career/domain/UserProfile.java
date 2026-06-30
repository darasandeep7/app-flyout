package com.atlas.career.domain;

import java.time.Instant;
import java.util.List;

public record UserProfile(
        String name,
        String email,
        String defaultPassword,
        String phone,
        String address,
        String linkedin,
        String github,
        String portfolio,
        String workAuthorization,
        String sponsorshipRequirement,
        String education,
        String employmentHistory,
        String resumePath,
        String coverLetterPath,
        List<String> savedAnswers,
        Instant updatedAt
) {
    public static UserProfile empty() {
        return new UserProfile("", "", "", "", "", "", "", "", "", "", "", "", "", "", List.of(), Instant.EPOCH);
    }
}
