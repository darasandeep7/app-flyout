package com.atlas.career.domain;

import java.util.Map;

public record MatchAssessment(
        int overallMatch,
        int resumeMatch,
        int javaMatch,
        int springMatch,
        int snowflakeMatch,
        int backendMatch,
        int leadershipMatch,
        int salaryMatch,
        int locationMatch,
        int visaMatch,
        int interviewProbability,
        Map<String, String> explanations
) {
}
