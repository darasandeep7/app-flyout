package com.atlas.plugin;

import java.util.List;

public interface AtlasPlugin {
    String id();

    String name();

    String description();

    List<PluginAction> actions();
}
