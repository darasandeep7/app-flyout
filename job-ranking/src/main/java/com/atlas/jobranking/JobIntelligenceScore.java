package com.atlas.jobranking;

import java.util.Map;

public record JobIntelligenceScore(
        int overallMatch,
        int technicalMatch,
        int javaMatch,
        int springMatch,
        int snowflakeMatch,
        int backendMatch,
        int microservicesMatch,
        int cloudMatch,
        int leadershipMatch,
        int salaryMatch,
        int locationMatch,
        int remoteMatch,
        int visaMatch,
        int careerGrowthScore,
        int interviewProbability,
        int confidence,
        Map<String, String> explanations
) {
}
