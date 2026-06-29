package com.atlas.recommendation;

import com.atlas.jobranking.JobIntelligenceScore;
import com.atlas.visa.VisaAnalysisResult;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RecommendationServiceTest {
    private final RecommendationService service = new RecommendationService();

    @Test
    void visaRiskBeatsTechnicalMatch() {
        JobRecommendation recommendation = service.recommend(
                new JobIntelligenceScore(90, 90, 90, 90, 80, 90, 85, 80, 85, 75, 90, 90, 10, 80, 70, 85, Map.of()),
                new VisaAnalysisResult(false, 10, 90, "No sponsorship", java.util.List.of("no sponsorship"), "Visa Risk", false, true),
                false,
                false
        );

        assertThat(recommendation.category()).isEqualTo(RecommendationCategory.VISA_RISK);
    }

    @Test
    void unrelatedJobsAreSkippedEvenWhenOtherSignalsLookFine() {
        JobRecommendation recommendation = service.recommend(
                new JobIntelligenceScore(72, 45, 35, 35, 45, 50, 45, 50, 55, 75, 90, 90, 85, 65, 60, 70, Map.of()),
                new VisaAnalysisResult(true, 85, 80, "ok", java.util.List.of(), "Review", false, true),
                false,
                false
        );

        assertThat(recommendation.category()).isEqualTo(RecommendationCategory.SKIP);
        assertThat(recommendation.explanation()).contains("Java");
    }
}
