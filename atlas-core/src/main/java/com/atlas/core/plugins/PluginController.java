package com.atlas.core.plugins;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/plugins")
public class PluginController {
    private final PluginRegistry registry;

    public PluginController(PluginRegistry registry) {
        this.registry = registry;
    }

    @GetMapping
    public List<PluginDescriptor> list() {
        return registry.all().stream()
                .map(plugin -> new PluginDescriptor(plugin.id(), plugin.name(), plugin.description(), plugin.actions()))
                .toList();
    }

    public record PluginDescriptor(String id, String name, String description, List<com.atlas.plugin.PluginAction> actions) {
    }
}
