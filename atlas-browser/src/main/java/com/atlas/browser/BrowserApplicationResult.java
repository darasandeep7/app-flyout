package com.atlas.browser;

import java.nio.file.Path;
import java.util.List;

public record BrowserApplicationResult(
        String status,
        String pauseReason,
        List<String> actions,
        List<Path> screenshots,
        boolean fallback,
        String error
) {
}
