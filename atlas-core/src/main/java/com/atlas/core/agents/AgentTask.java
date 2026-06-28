package com.atlas.core.agents;

import java.util.Map;

public record AgentTask(String projectId, String objective, Map<String, Object> context) {
}
