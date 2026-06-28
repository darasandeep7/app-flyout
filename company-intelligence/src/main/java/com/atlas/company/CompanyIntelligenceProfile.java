package com.atlas.company;

import java.time.Instant;
import java.util.List;

public record CompanyIntelligenceProfile(
        String id,
        String companyName,
        String industry,
        String website,
        String careersUrl,
        String knownAtsPlatform,
        List<String> locations,
        String remotePolicy,
        int visaSponsorshipConfidence,
        String historicalSponsorship,
        int historicalApplications,
        int historicalInterviews,
        int historicalRejections,
        int historicalOffers,
        int averageMatchScore,
        String recruiterNotes,
        boolean blocked,
        int priority,
        Instant lastScan,
        Instant lastUpdated,
        int confidenceScore,
        List<String> technologyStack
) {
}
