package com.atlas.core.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "atlas")
public record AtlasProperties(
        @NotBlank String workspaceFolder,
        @NotBlank String outputFolder,
        @NotBlank String promptFolder,
        boolean developerMode,
        Ollama ollama,
        Browser browser
) {
    public record Ollama(String baseUrl, String defaultTextModel, String defaultVisionModel) {
    }

    public record Browser(String pythonPath, String browserUseEntrypoint, int playwrightTimeout) {
    }
}
