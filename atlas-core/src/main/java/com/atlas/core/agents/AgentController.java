package com.atlas.core.agents;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/agents")
public class AgentController {
    private final AgentRegistry registry;

    public AgentController(AgentRegistry registry) {
        this.registry = registry;
    }

    @GetMapping
    public List<AgentDescriptor> list() {
        return registry.all().stream().map(agent -> new AgentDescriptor(agent.id(), agent.name())).toList();
    }

    public record AgentDescriptor(String id, String name) {
    }
}
