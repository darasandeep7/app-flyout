package com.atlas.career;

import com.atlas.plugin.AtlasPlugin;
import com.atlas.plugin.PluginAction;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class CareerPlugin implements AtlasPlugin {
    @Override
    public String id() {
        return "career";
    }

    @Override
    public String name() {
        return "Career Copilot";
    }

    @Override
    public String description() {
        return "Local career assistant for company tracking, job analysis, visa intelligence, and application preparation.";
    }

    @Override
    public List<PluginAction> actions() {
        return List.of(
                new PluginAction("dashboard", "Open dashboard", "Review new jobs, matches, and application readiness."),
                new PluginAction("add-company", "Add company", "Track a company career page and visa history."),
                new PluginAction("analyze-job", "Analyze job", "Score a job description for match and visa fit.")
        );
    }
}
