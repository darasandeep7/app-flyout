package com.atlas.career.domain;

public record CareerLearningInsight(
        String company,
        int applied,
        int interviews,
        int offers,
        int rejections,
        int blocked,
        int score,
        String recommendation
) {
}
