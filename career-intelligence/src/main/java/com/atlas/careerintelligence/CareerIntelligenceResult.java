package com.atlas.careerintelligence;

import com.atlas.jobranking.JobIntelligenceScore;
import com.atlas.recommendation.JobRecommendation;
import com.atlas.visa.VisaAnalysisResult;

public record CareerIntelligenceResult(
        VisaAnalysisResult visa,
        JobIntelligenceScore ranking,
        JobRecommendation recommendation
) {
}
