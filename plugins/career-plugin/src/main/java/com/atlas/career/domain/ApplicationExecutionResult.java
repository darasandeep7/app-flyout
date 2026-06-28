package com.atlas.career.domain;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

public record ApplicationExecutionResult(
        String applicationId,
        String status,
        String pauseReason,
        List<String> actions,
        List<Path> screenshots,
        boolean fallback,
        String error,
        Instant executedAt
) {
}
