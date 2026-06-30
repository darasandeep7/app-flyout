package com.atlas.career.domain;

import java.time.Instant;
import java.util.Map;

public record MemoryRecord(
        String id,
        String type,
        String scope,
        String key,
        String intent,
        String question,
        String answer,
        String company,
        String ats,
        Map<String, Object> data,
        int confidence,
        int usageCount,
        String source,
        Instant createdAt,
        Instant lastUsed,
        Instant updatedAt
) {
}
