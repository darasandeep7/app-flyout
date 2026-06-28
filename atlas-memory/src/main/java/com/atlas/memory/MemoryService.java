package com.atlas.memory;

import com.atlas.storage.ProjectRecord;
import com.atlas.storage.ProjectStorage;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class MemoryService {
    private final ProjectStorage storage;

    public MemoryService(ProjectStorage storage) {
        this.storage = storage;
    }

    public void remember(ProjectRecord project, String type, String content, Map<String, Object> metadata) {
        List<MemoryEntry> entries = new ArrayList<>();
        entries.add(new MemoryEntry(project.id(), type, content, metadata, Instant.now()));
        storage.writeJson(project, "memory/" + Instant.now().toEpochMilli() + "-" + type + ".json", entries.getFirst());
    }
}
