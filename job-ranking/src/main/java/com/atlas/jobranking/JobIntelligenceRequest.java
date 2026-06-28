package com.atlas.jobranking;

import com.atlas.visa.VisaAnalysisResult;
import java.time.Instant;
import java.util.List;

public record JobIntelligenceRequest(
        String company,
        String title,
        String location,
        String salary,
        String remoteStatus,
        Instant postingDate,
        String description,
        List<String> requiredSkills,
        String experienceLevel,
        VisaAnalysisResult visa
) {
}
