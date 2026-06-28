package com.atlas.memory;

import java.time.Instant;
import java.util.Map;

public record MemoryEntry(String projectId, String type, String content, Map<String, Object> metadata, Instant createdAt) {
}
