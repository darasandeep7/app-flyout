package com.atlas.core.plugins;

import com.atlas.plugin.AtlasPlugin;
import com.atlas.plugin.PluginAction;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PluginRegistryTest {
    @Test
    void returnsPluginsSortedByName() {
        AtlasPlugin b = plugin("b", "Beta");
        AtlasPlugin a = plugin("a", "Alpha");

        PluginRegistry registry = new PluginRegistry(List.of(b, a));

        assertThat(registry.all()).extracting(AtlasPlugin::name).containsExactly("Alpha", "Beta");
    }

    private AtlasPlugin plugin(String id, String name) {
        return new AtlasPlugin() {
            @Override public String id() { return id; }
            @Override public String name() { return name; }
            @Override public String description() { return ""; }
            @Override public List<PluginAction> actions() { return List.of(); }
        };
    }
}
