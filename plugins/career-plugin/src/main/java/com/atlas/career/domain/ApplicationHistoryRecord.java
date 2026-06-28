package com.atlas.career.domain;

import java.time.Instant;
import java.util.List;

public record ApplicationHistoryRecord(
        String applicationId,
        String jobId,
        String company,
        String title,
        String status,
        String note,
        String resumeVersion,
        String resumePath,
        String coverLetterPath,
        String answersPath,
        List<String> questionsAsked,
        Instant recordedAt
) {
}
