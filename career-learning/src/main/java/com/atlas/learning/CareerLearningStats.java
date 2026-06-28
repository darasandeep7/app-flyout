package com.atlas.learning;

public record CareerLearningStats(
        int jobsApplied,
        int jobsIgnored,
        int jobsSkipped,
        int interviews,
        int offers,
        int rejections,
        int ghostedApplications,
        int recruiterMessages,
        int resumeVersionsUsed,
        int coverLettersUsed,
        int userFeedbackItems
) {
}
