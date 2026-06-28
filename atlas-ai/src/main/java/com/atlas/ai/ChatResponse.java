package com.atlas.ai;

import java.time.Duration;
import java.util.Map;

public record ChatResponse(
        String model,
        String text,
        Duration elapsed,
        Map<String, Object> raw,
        boolean fallback,
        String error
) {
}
