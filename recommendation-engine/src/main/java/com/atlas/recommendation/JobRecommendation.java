package com.atlas.recommendation;

public record JobRecommendation(
        RecommendationCategory category,
        int confidence,
        String explanation,
        boolean userOverrideAllowed
) {
}
