package com.atlas.property;

import com.atlas.plugin.AtlasPlugin;
import com.atlas.plugin.PluginAction;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class PropertyPlugin implements AtlasPlugin {
    @Override
    public String id() {
        return "property";
    }

    @Override
    public String name() {
        return "Property Plugin";
    }

    @Override
    public String description() {
        return "Extracts public property listing details and generates local marketing outputs.";
    }

    @Override
    public List<PluginAction> actions() {
        return List.of(new PluginAction("analyze-listing", "Analyze listing", "Paste a public listing URL and generate a local project package."));
    }
}
