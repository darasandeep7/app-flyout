package com.atlas.storage;

import java.nio.file.Path;
import java.time.Instant;

public record ProjectRecord(String id, String name, String pluginId, Path folder, Instant createdAt) {
}
