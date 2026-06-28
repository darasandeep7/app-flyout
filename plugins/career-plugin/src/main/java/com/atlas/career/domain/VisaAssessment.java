package com.atlas.career.domain;

import java.util.List;

public record VisaAssessment(
        int score,
        int confidence,
        String recommendation,
        String reason,
        List<String> detectedSignals
) {
}
