package com.atlas.career.domain;

import java.time.Instant;
import java.util.List;

public record CompanyRecord(
        String id,
        String name,
        String industry,
        String website,
        String careerUrl,
        String atsPlatform,
        String remotePolicy,
        String hiringStatus,
        String visaSponsorshipHistory,
        int visaConfidence,
        List<String> locations,
        int historicalApplications,
        int historicalInterviews,
        int historicalRejections,
        int historicalOffers,
        int averageMatchScore,
        int priority,
        boolean blocked,
        Instant lastScan,
        Instant lastUpdated,
        int confidenceScore,
        String notes,
        List<String> technologyStack,
        List<String> learningHistory
) {
}
