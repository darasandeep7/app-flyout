package com.atlas.career.domain;

import java.time.Instant;
import java.util.List;

public record ApplicationPackage(
        String id,
        String jobId,
        String company,
        String title,
        String status,
        int recommendationConfidence,
        String recommendation,
        String resumeVersion,
        String resumePath,
        String coverLetterPath,
        String answersPath,
        String reportPath,
        List<ApplicationQuestionAnswer> answers,
        Instant createdAt,
        Instant updatedAt
) {
}
