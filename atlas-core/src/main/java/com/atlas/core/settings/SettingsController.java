package com.atlas.core.settings;

import com.atlas.ai.AiModelPreferences;
import com.atlas.ai.AiModelSettingsService;
import com.atlas.ai.ChatRequest;
import com.atlas.ai.ChatResponse;
import com.atlas.ai.ModelCatalog;
import com.atlas.ai.ModelProvider;
import com.atlas.core.config.AtlasProperties;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/settings")
public class SettingsController {
    private final AtlasProperties properties;
    private final ModelCatalog modelCatalog;
    private final ModelProvider modelProvider;
    private final AiModelSettingsService aiModelSettings;

    public SettingsController(AtlasProperties properties, ModelCatalog modelCatalog, ModelProvider modelProvider, AiModelSettingsService aiModelSettings) {
        this.properties = properties;
        this.modelCatalog = modelCatalog;
        this.modelProvider = modelProvider;
        this.aiModelSettings = aiModelSettings;
    }

    @GetMapping
    public SettingsView get() {
        return new SettingsView(properties, modelCatalog.availableModels());
    }

    @GetMapping("/ai-models")
    public AiModelSettingsView aiModels() {
        return new AiModelSettingsView(aiModelSettings.preferences(), modelCatalog.availableModels());
    }

    @PostMapping("/ai-models")
    public AiModelSettingsView saveAiModels(@RequestBody AiModelPreferences preferences) {
        return new AiModelSettingsView(aiModelSettings.save(preferences), modelCatalog.availableModels());
    }

    @PostMapping("/ai-models/test")
    public ChatResponse testModel(@RequestBody ModelTestRequest request) {
        String prompt = request.prompt() == null || request.prompt().isBlank()
                ? "Reply with one short sentence confirming this model is working."
                : request.prompt();
        return modelProvider.generate(new ChatRequest(request.model(), "fastFallback", prompt, java.util.Map.of("temperature", 0.1)));
    }

    public record AiModelSettingsView(AiModelPreferences preferences, java.util.List<com.atlas.ai.LocalModel> models) {
    }

    public record ModelTestRequest(String model, String prompt) {
    }
}
