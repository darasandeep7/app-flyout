package com.atlas.ai;

public interface ModelProvider {
    ChatResponse generate(ChatRequest request);
}
