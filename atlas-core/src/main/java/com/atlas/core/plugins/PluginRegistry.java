package com.atlas.core.plugins;

import com.atlas.plugin.AtlasPlugin;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class PluginRegistry {
    private final List<AtlasPlugin> plugins;

    public PluginRegistry(List<AtlasPlugin> plugins) {
        this.plugins = plugins.stream()
                .sorted(Comparator.comparing(AtlasPlugin::name))
                .toList();
    }

    public List<AtlasPlugin> all() {
        return plugins;
    }
}
