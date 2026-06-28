package com.atlas.browser;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public record BrowserResult(
        String url,
        String title,
        String visibleText,
        List<Path> screenshots,
        List<Path> images,
        Map<String, Object> structured,
        boolean fallback,
        String error
) {
}
