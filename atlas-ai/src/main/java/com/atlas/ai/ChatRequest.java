package com.atlas.ai;

import java.util.Map;

public record ChatRequest(String model, String prompt, Map<String, Object> options) {
}
