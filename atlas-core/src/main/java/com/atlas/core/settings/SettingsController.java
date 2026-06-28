package com.atlas.core.settings;

import com.atlas.ai.ModelCatalog;
import com.atlas.core.config.AtlasProperties;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/settings")
public class SettingsController {
    private final AtlasProperties properties;
    private final ModelCatalog modelCatalog;

    public SettingsController(AtlasProperties properties, ModelCatalog modelCatalog) {
        this.properties = properties;
        this.modelCatalog = modelCatalog;
    }

    @GetMapping
    public SettingsView get() {
        return new SettingsView(properties, modelCatalog.availableModels());
    }
}
