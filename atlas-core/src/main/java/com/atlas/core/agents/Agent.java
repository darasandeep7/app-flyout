package com.atlas.core.agents;

public interface Agent {
    String id();

    String name();

    AgentResult run(AgentTask task);
}
