package com.atlas.career.domain;

import java.time.Instant;

public record AnswerTrainingRule(
        String id,
        String questionPattern,
        String preferredFormat,
        String exampleAnswer,
        boolean enabled,
        Instant createdAt,
        Instant updatedAt
) {
}
