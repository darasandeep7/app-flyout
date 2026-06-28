package com.atlas.core.settings;

import com.atlas.ai.LocalModel;
import com.atlas.core.config.AtlasProperties;
import java.util.List;

public record SettingsView(AtlasProperties atlas, List<LocalModel> models) {
}
