package com.atlas.core.projects;

import com.atlas.storage.ProjectRecord;
import com.atlas.storage.ProjectStorage;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {
    private final ProjectStorage storage;

    public ProjectController(ProjectStorage storage) {
        this.storage = storage;
    }

    @GetMapping
    public List<ProjectRecord> list() {
        return storage.listProjects();
    }
}
