package com.atlas.ai;

import java.util.Map;

public record ChatRequest(String model, String task, String prompt, Map<String, Object> options) {
    public ChatRequest(String model, String prompt, Map<String, Object> options) {
        this(model, null, prompt, options);
    }
}
