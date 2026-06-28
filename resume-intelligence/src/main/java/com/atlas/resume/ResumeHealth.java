package com.atlas.resume;

import java.util.List;

public record ResumeHealth(
        int atsScore,
        int resumeHealthScore,
        List<String> missingKeywords,
        List<String> strengths,
        List<String> weaknesses,
        List<String> improvementSuggestions,
        String safetyPolicy
) {
}
