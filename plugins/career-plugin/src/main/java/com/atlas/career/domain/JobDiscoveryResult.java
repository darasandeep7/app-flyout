package com.atlas.career.domain;

import java.time.Instant;
import java.util.List;

public record JobDiscoveryResult(
        Instant scannedAt,
        int companiesScanned,
        int jobsFound,
        int jobsSaved,
        int expiredRemoved,
        List<String> messages
) {
}
