package com.atlas.core.agents;

import java.time.Duration;
import java.util.Map;

public record AgentResult(String agentId, String summary, Map<String, Object> output, Duration elapsed, boolean success) {
}
