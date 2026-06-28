package com.atlas.careerintelligence;

import com.atlas.jobranking.JobIntelligenceRequest;
import com.atlas.jobranking.JobIntelligenceScore;
import com.atlas.jobranking.JobRankingService;
import com.atlas.recommendation.JobRecommendation;
import com.atlas.recommendation.RecommendationService;
import com.atlas.visa.VisaAnalysisRequest;
import com.atlas.visa.VisaAnalysisResult;
import com.atlas.visa.VisaIntelligenceAnalyzer;
import org.springframework.stereotype.Service;

@Service
public class CareerIntelligenceEngine {
    private final VisaIntelligenceAnalyzer visaIntelligence;
    private final JobRankingService jobRanking;
    private final RecommendationService recommendationService;

    public CareerIntelligenceEngine(VisaIntelligenceAnalyzer visaIntelligence, JobRankingService jobRanking, RecommendationService recommendationService) {
        this.visaIntelligence = visaIntelligence;
        this.jobRanking = jobRanking;
        this.recommendationService = recommendationService;
    }

    public CareerIntelligenceResult evaluate(VisaAnalysisRequest visaRequest, JobIntelligenceRequest rankingRequest, boolean blockedCompany, boolean alreadyApplied) {
        VisaAnalysisResult visa = visaIntelligence.analyze(visaRequest);
        JobIntelligenceRequest enriched = new JobIntelligenceRequest(
                rankingRequest.company(),
                rankingRequest.title(),
                rankingRequest.location(),
                rankingRequest.salary(),
                rankingRequest.remoteStatus(),
                rankingRequest.postingDate(),
                rankingRequest.description(),
                rankingRequest.requiredSkills(),
                rankingRequest.experienceLevel(),
                visa
        );
        JobIntelligenceScore score = jobRanking.score(enriched);
        JobRecommendation recommendation = recommendationService.recommend(score, visa, blockedCompany, alreadyApplied);
        return new CareerIntelligenceResult(visa, score, recommendation);
    }
}
