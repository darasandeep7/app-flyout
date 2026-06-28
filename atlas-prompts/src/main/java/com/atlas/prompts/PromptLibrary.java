package com.atlas.prompts;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class PromptLibrary {
    private final Path promptFolder;

    public PromptLibrary(@Value("${atlas.prompt-folder:prompts}") String promptFolder) {
        this.promptFolder = Path.of(promptFolder).normalize();
    }

    public String read(String name) {
        Path path = promptFolder.resolve(name).normalize();
        if (!path.startsWith(promptFolder)) {
            throw new IllegalArgumentException("Invalid prompt path");
        }
        try {
            return Files.readString(path);
        } catch (IOException ex) {
            throw new IllegalStateException("Could not read prompt " + name, ex);
        }
    }
}
