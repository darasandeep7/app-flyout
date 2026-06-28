package com.atlas.storage;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public interface ProjectStorage {
    ProjectRecord createProject(String pluginId, String name);

    void writeJson(ProjectRecord project, String relativePath, Object payload);

    void writeText(ProjectRecord project, String relativePath, String text);

    List<ProjectRecord> listProjects();

    Path resolve(ProjectRecord project, String relativePath);

    Map<String, Object> diagnostics();
}
