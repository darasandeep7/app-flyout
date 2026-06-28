package com.atlas.career.domain;

import java.time.Instant;
import java.util.List;

public record JobRecord(
        String id,
        String companyId,
        String company,
        String title,
        String location,
        String url,
        String description,
        VisaAssessment visa,
        MatchAssessment match,
        JobIntelligence intelligence,
        String applicationStatus,
        boolean resumeReady,
        boolean coverLetterReady,
        Instant discoveredAt,
        Instant updatedAt,
        List<String> notes
) {
}
