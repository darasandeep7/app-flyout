package com.atlas.career.domain;

public record ApplicationQuestionAnswer(
        String question,
        String answer,
        String source,
        boolean needsReview
) {
}
