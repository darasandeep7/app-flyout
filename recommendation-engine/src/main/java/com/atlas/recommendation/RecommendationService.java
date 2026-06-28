package com.atlas.recommendation;

import com.atlas.jobranking.JobIntelligenceScore;
import com.atlas.visa.VisaAnalysisResult;
import org.springframework.stereotype.Service;

@Service
public class RecommendationService {
    public JobRecommendation recommend(JobIntelligenceScore score, VisaAnalysisResult visa, boolean blockedCompany, boolean alreadyApplied) {
        if (blockedCompany) {
            return new JobRecommendation(RecommendationCategory.BLOCKED_COMPANY, 95, "Company is blocked in your preferences.", true);
        }
        if (alreadyApplied) {
            return new JobRecommendation(RecommendationCategory.ALREADY_APPLIED, 98, "Application history shows this role or company has already been applied to.", false);
        }
        if (visa != null && !visa.visaEligible() && visa.visaConfidence() >= 70) {
            return new JobRecommendation(RecommendationCategory.VISA_RISK, visa.visaConfidence(), visa.reason(), true);
        }
        if (score.overallMatch() >= 86 && score.interviewProbability() >= 70) {
            return new JobRecommendation(RecommendationCategory.APPLY_TODAY, score.confidence(), "High match and strong interview probability.", true);
        }
        if (score.overallMatch() >= 80) {
            return new JobRecommendation(RecommendationCategory.EXCELLENT_MATCH, score.confidence(), "Excellent match across technical and preference signals.", true);
        }
        if (score.overallMatch() >= 70) {
            return new JobRecommendation(RecommendationCategory.GOOD_MATCH, score.confidence(), "Good match, but review weaker signals before applying.", true);
        }
        if (score.confidence() < 50) {
            return new JobRecommendation(RecommendationCategory.NEEDS_REVIEW, score.confidence(), "Low confidence; inspect the job manually.", true);
        }
        if (score.overallMatch() >= 60) {
            return new JobRecommendation(RecommendationCategory.INTERESTING, score.confidence(), "Potentially useful but not a priority.", true);
        }
        return new JobRecommendation(RecommendationCategory.SKIP, score.confidence(), "Low overall fit.", true);
    }
}
