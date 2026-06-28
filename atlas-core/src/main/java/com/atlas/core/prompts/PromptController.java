package com.atlas.core.prompts;

import com.atlas.core.config.AtlasProperties;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/prompts")
public class PromptController {
    private final Path promptFolder;

    public PromptController(AtlasProperties properties) {
        this.promptFolder = Path.of(properties.promptFolder());
    }

    @GetMapping
    public List<String> list() throws IOException {
        if (Files.notExists(promptFolder)) {
            return List.of();
        }
        try (var stream = Files.list(promptFolder)) {
            return stream.filter(path -> path.getFileName().toString().endsWith(".md"))
                    .map(path -> path.getFileName().toString())
                    .sorted()
                    .toList();
        }
    }

    @GetMapping("/{name}")
    public String read(@PathVariable String name) throws IOException {
        Path path = promptFolder.resolve(name).normalize();
        if (!path.startsWith(promptFolder.normalize())) {
            throw new IllegalArgumentException("Invalid prompt path");
        }
        return Files.readString(path);
    }
}
