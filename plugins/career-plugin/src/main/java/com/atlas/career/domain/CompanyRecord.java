package com.atlas.career.domain;

import java.time.Instant;
import java.util.List;

public record CompanyRecord(
        String id,
        String name,
        String careerUrl,
        String atsPlatform,
        String hiringStatus,
        String visaSponsorshipHistory,
        int visaConfidence,
        List<String> locations,
        int priority,
        boolean blocked,
        Instant lastScan,
        String notes,
        List<String> learningHistory
) {
}
