package com.atlas.resume;

import java.util.List;

public record ResumeProfile(
        String masterResume,
        List<String> preferredSkills,
        List<String> preferredKeywords,
        List<String> resumeVersions
) {
}
