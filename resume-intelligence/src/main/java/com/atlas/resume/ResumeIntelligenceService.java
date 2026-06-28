package com.atlas.resume;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
public class ResumeIntelligenceService {
    public ResumeHealth health(ResumeProfile profile, String targetJobDescription) {
        List<String> preferred = profile == null || profile.preferredKeywords() == null
                ? List.of("Java", "Spring", "Backend", "Microservices", "Cloud", "Leadership")
                : profile.preferredKeywords();
        String resume = profile == null || profile.masterResume() == null ? "" : profile.masterResume().toLowerCase(Locale.ROOT);
        String job = targetJobDescription == null ? "" : targetJobDescription.toLowerCase(Locale.ROOT);
        List<String> missing = new ArrayList<>();
        List<String> strengths = new ArrayList<>();
        for (String keyword : preferred) {
            String normalized = keyword.toLowerCase(Locale.ROOT);
            if (resume.contains(normalized)) {
                strengths.add(keyword);
            } else if (job.isBlank() || job.contains(normalized)) {
                missing.add(keyword);
            }
        }
        int ats = Math.max(35, Math.min(95, 95 - missing.size() * 8));
        int health = Math.max(40, Math.min(95, 70 + strengths.size() * 4 - missing.size() * 5));
        return new ResumeHealth(
                ats,
                health,
                missing,
                strengths,
                missing.isEmpty() ? List.of("Add quantified impact where possible.") : List.of("Important target keywords are underrepresented."),
                List.of("Reorder bullets for the target role.", "Emphasize existing experience only.", "Do not invent projects, employers, metrics, or tools."),
                "Never fabricate experience. Only reorganize, rephrase, and emphasize existing experience."
        );
    }
}
