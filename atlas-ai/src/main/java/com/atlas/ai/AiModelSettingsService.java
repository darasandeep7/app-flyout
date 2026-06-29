package com.atlas.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class AiModelSettingsService {
    private final Path settingsPath;
    private final ObjectMapper objectMapper;

    public AiModelSettingsService(@Value("${atlas.workspace-folder:workspace}") String workspaceFolder, ObjectMapper objectMapper) {
        this.settingsPath = Path.of(workspaceFolder).resolve("settings/ai-models.json");
        this.objectMapper = objectMapper.findAndRegisterModules();
    }

    public AiModelPreferences preferences() {
        if (Files.notExists(settingsPath)) {
            return save(AiModelPreferences.defaults());
        }
        try {
            return fillBlanks(objectMapper.readValue(settingsPath.toFile(), AiModelPreferences.class));
        } catch (IOException ex) {
            return AiModelPreferences.defaults();
        }
    }

    public AiModelPreferences save(AiModelPreferences preferences) {
        AiModelPreferences safe = fillBlanks(preferences);
        try {
            Files.createDirectories(settingsPath.getParent());
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(settingsPath.toFile(), safe);
            return safe;
        } catch (IOException ex) {
            throw new IllegalStateException("Could not save AI model preferences", ex);
        }
    }

    public String modelFor(String task) {
        return preferences().modelFor(task);
    }

    private AiModelPreferences fillBlanks(AiModelPreferences value) {
        AiModelPreferences defaults = AiModelPreferences.defaults();
        if (value == null) {
            return defaults;
        }
        return new AiModelPreferences(
                blankToDefault(value.resumeGeneration(), defaults.resumeGeneration()),
                blankToDefault(value.coverLetters(), defaults.coverLetters()),
                blankToDefault(value.applicationAnswers(), defaults.applicationAnswers()),
                blankToDefault(value.jobAnalysis(), defaults.jobAnalysis()),
                blankToDefault(value.visaReasoning(), defaults.visaReasoning()),
                blankToDefault(value.codeGeneration(), defaults.codeGeneration()),
                blankToDefault(value.fastFallback(), defaults.fastFallback())
        );
    }

    private String blankToDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
