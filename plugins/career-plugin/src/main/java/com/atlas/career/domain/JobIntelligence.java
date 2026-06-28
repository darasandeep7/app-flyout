package com.atlas.career.domain;

import com.atlas.jobranking.DuplicateAssessment;
import com.atlas.jobranking.JobIntelligenceScore;
import com.atlas.recommendation.JobRecommendation;
import com.atlas.visa.VisaAnalysisResult;

public record JobIntelligence(
        VisaAnalysisResult visa,
        JobIntelligenceScore ranking,
        JobRecommendation recommendation,
        DuplicateAssessment duplicate
) {
}
