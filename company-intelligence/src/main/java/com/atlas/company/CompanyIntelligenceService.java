package com.atlas.company;

import com.atlas.common.Slug;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class CompanyIntelligenceService {
    public CompanyIntelligenceProfile profile(String name, String careersUrl, List<String> locations, String notes, int priority) {
        return new CompanyIntelligenceProfile(
                Slug.of(name),
                name,
                "Unknown",
                websiteFrom(careersUrl),
                careersUrl,
                detectAts(careersUrl),
                locations == null ? List.of() : locations,
                "Unknown",
                45,
                "Learning from scans and user outcomes",
                0,
                0,
                0,
                0,
                0,
                notes,
                false,
                priority,
                Instant.EPOCH,
                Instant.now(),
                40,
                List.of()
        );
    }

    public CompanyIntelligenceProfile updateAfterScan(CompanyIntelligenceProfile profile, int jobsFound, int averageMatchScore, List<String> detectedStack) {
        int confidence = Math.min(95, Math.max(profile.confidenceScore(), 45 + jobsFound * 5));
        return new CompanyIntelligenceProfile(
                profile.id(),
                profile.companyName(),
                profile.industry(),
                profile.website(),
                profile.careersUrl(),
                profile.knownAtsPlatform(),
                profile.locations(),
                profile.remotePolicy(),
                profile.visaSponsorshipConfidence(),
                profile.historicalSponsorship(),
                profile.historicalApplications(),
                profile.historicalInterviews(),
                profile.historicalRejections(),
                profile.historicalOffers(),
                averageMatchScore,
                profile.recruiterNotes(),
                profile.blocked(),
                profile.priority(),
                Instant.now(),
                Instant.now(),
                confidence,
                detectedStack == null ? profile.technologyStack() : detectedStack
        );
    }

    private String websiteFrom(String careersUrl) {
        if (careersUrl == null || careersUrl.isBlank()) {
            return "";
        }
        return careersUrl.replaceFirst("(/careers.*|/jobs.*|/workday.*)$", "");
    }

    private String detectAts(String careersUrl) {
        String value = careersUrl == null ? "" : careersUrl.toLowerCase();
        if (value.contains("workday")) {
            return "Workday";
        }
        if (value.contains("greenhouse")) {
            return "Greenhouse";
        }
        if (value.contains("lever")) {
            return "Lever";
        }
        if (value.contains("ashby")) {
            return "Ashby";
        }
        return "Unknown";
    }
}
