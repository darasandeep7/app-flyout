package com.atlas.browser;

import java.nio.file.Path;

public record BrowserRequest(String url, Path projectFolder, boolean captureScreenshots, boolean captureImages) {
}
