package com.atlas.core.agents;

import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class AgentRegistry {
    private final List<Agent> agents;

    public AgentRegistry(List<Agent> agents) {
        this.agents = agents.stream().sorted(Comparator.comparing(Agent::name)).toList();
    }

    public List<Agent> all() {
        return agents;
    }
}
