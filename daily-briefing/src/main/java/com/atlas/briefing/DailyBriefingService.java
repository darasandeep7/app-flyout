package com.atlas.briefing;

import com.atlas.learning.CareerLearningService;
import com.atlas.resume.ResumeHealth;
import com.atlas.resume.ResumeIntelligenceService;
import com.atlas.resume.ResumeProfile;
import java.time.LocalTime;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class DailyBriefingService {
    private final ResumeIntelligenceService resumeIntelligence;
    private final CareerLearningService learning;

    public DailyBriefingService(ResumeIntelligenceService resumeIntelligence, CareerLearningService learning) {
        this.resumeIntelligence = resumeIntelligence;
        this.learning = learning;
    }

    public DailyBriefing briefing(int jobsFoundToday, int excellentMatches, int visaFriendlyJobs, int applicationsReady, List<String> topJobs, List<String> topCompanies) {
        ResumeHealth resumeHealth = resumeIntelligence.health(new ResumeProfile("", List.of(), List.of("Java", "Spring", "Backend", "Leadership"), List.of()), "");
        String greeting = LocalTime.now().isBefore(LocalTime.NOON) ? "Good Morning" : "Career Briefing";
        return new DailyBriefing(greeting, jobsFoundToday, excellentMatches, visaFriendlyJobs, applicationsReady, 0, topCompanies.size(), 0, topJobs, topCompanies, resumeHealth, learning.emptyStats());
    }
}
