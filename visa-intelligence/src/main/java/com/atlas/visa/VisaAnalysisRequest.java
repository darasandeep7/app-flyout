package com.atlas.visa;

import java.util.List;

public record VisaAnalysisRequest(
        String company,
        String title,
        String description,
        int companyVisaConfidence,
        List<String> companyHistory
) {
}
