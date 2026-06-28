package com.atlas.visa;

import java.util.List;

public record VisaAnalysisResult(
        boolean visaEligible,
        int visaScore,
        int visaConfidence,
        String reason,
        List<String> supportingEvidence,
        String recommendation,
        boolean needsManualReview,
        boolean userOverrideAllowed
) {
}
