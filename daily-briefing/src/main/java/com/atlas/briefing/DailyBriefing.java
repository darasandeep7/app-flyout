package com.atlas.briefing;

import com.atlas.learning.CareerLearningStats;
import com.atlas.resume.ResumeHealth;
import java.util.List;

public record DailyBriefing(
        String greeting,
        int jobsFoundToday,
        int excellentMatches,
        int visaFriendlyJobs,
        int applicationsReady,
        int interviewsScheduled,
        int companiesRecentlyAdded,
        int companiesRequiringReview,
        List<String> topRecommendedJobs,
        List<String> topCompaniesHiring,
        ResumeHealth resumeHealth,
        CareerLearningStats applicationFunnel
) {
}
