package com.atlas.storage;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class LocalProjectStorage implements ProjectStorage {
    private final Path workspace;
    private final ObjectMapper objectMapper;

    public LocalProjectStorage(@Value("${atlas.workspace-folder:workspace}") String workspace, ObjectMapper objectMapper) {
        this.workspace = Path.of(workspace);
        this.objectMapper = objectMapper;
        initialize();
    }

    @Override
    public ProjectRecord createProject(String pluginId, String name) {
        String slug = name.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
        String id = Instant.now().toString().replaceAll("[^0-9]", "") + "-" + UUID.randomUUID().toString().substring(0, 8);
        Path folder = workspace.resolve(pluginId).resolve(id + "-" + slug);
        try {
            Files.createDirectories(folder.resolve("images"));
            Files.createDirectories(folder.resolve("reports"));
            Files.createDirectories(folder.resolve("logs"));
            Files.createDirectories(folder.resolve("prompts"));
            ProjectRecord record = new ProjectRecord(id, name, pluginId, folder, Instant.now());
            writeJson(record, "project.json", record);
            index(record);
            return record;
        } catch (IOException ex) {
            throw new IllegalStateException("Could not create project folder " + folder, ex);
        }
    }

    @Override
    public void writeJson(ProjectRecord project, String relativePath, Object payload) {
        try {
            Path path = resolve(project, relativePath);
            Files.createDirectories(path.getParent());
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(path.toFile(), payload);
        } catch (IOException ex) {
            throw new IllegalStateException("Could not write JSON " + relativePath, ex);
        }
    }

    @Override
    public void writeText(ProjectRecord project, String relativePath, String text) {
        try {
            Path path = resolve(project, relativePath);
            Files.createDirectories(path.getParent());
            Files.writeString(path, text);
        } catch (IOException ex) {
            throw new IllegalStateException("Could not write text " + relativePath, ex);
        }
    }

    @Override
    public List<ProjectRecord> listProjects() {
        if (Files.notExists(workspace)) {
            return List.of();
        }
        try (var stream = Files.walk(workspace, 4)) {
            return stream.filter(path -> path.getFileName().toString().equals("project.json"))
                    .map(this::readProject)
                    .sorted(Comparator.comparing(ProjectRecord::createdAt).reversed())
                    .toList();
        } catch (IOException ex) {
            return List.of();
        }
    }

    @Override
    public Path resolve(ProjectRecord project, String relativePath) {
        return project.folder().resolve(relativePath).normalize();
    }

    @Override
    public Map<String, Object> diagnostics() {
        return Map.of("workspace", workspace.toAbsolutePath().toString(), "sqlite", workspace.resolve("atlas.db").toString());
    }

    private ProjectRecord readProject(Path path) {
        try {
            return objectMapper.readValue(path.toFile(), ProjectRecord.class);
        } catch (IOException ex) {
            return new ProjectRecord(path.getParent().getFileName().toString(), path.getParent().getFileName().toString(), "unknown", path.getParent(), Instant.EPOCH);
        }
    }

    private void initialize() {
        try {
            Files.createDirectories(workspace);
            try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + workspace.resolve("atlas.db"));
                 Statement statement = connection.createStatement()) {
                statement.executeUpdate("""
                        create table if not exists projects (
                          id text primary key,
                          name text not null,
                          plugin_id text not null,
                          folder text not null,
                          created_at text not null
                        )
                        """);
            }
        } catch (IOException | SQLException ex) {
            throw new IllegalStateException("Could not initialize local storage", ex);
        }
    }

    private void index(ProjectRecord record) {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + workspace.resolve("atlas.db"));
             PreparedStatement statement = connection.prepareStatement("insert or replace into projects values (?,?,?,?,?)")) {
            statement.setString(1, record.id());
            statement.setString(2, record.name());
            statement.setString(3, record.pluginId());
            statement.setString(4, record.folder().toString());
            statement.setString(5, record.createdAt().toString());
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("Could not index project", ex);
        }
    }
}
