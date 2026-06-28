package com.atlas.property;

import com.atlas.browser.BrowserResult;
import com.atlas.storage.ProjectRecord;
import java.util.List;

public record PropertyAnalysisResult(
        ProjectRecord project,
        BrowserResult browser,
        String improvedTitle,
        String improvedDescription,
        String marketingSummary,
        int listingScore,
        int photoScore,
        List<String> suggestions,
        List<String> videoStoryboard,
        String brochurePath,
        String packagePath
) {
}
