package com.atlas.browser;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class BrowserUseAutomation implements BrowserAutomation {
    private final String pythonPath;
    private final String entrypoint;
    private final ObjectMapper objectMapper;

    public BrowserUseAutomation(
            @Value("${atlas.browser.python-path:python3}") String pythonPath,
            @Value("${atlas.browser.browser-use-entrypoint:scripts/browser_use_worker.py}") String entrypoint,
            ObjectMapper objectMapper
    ) {
        this.pythonPath = pythonPath;
        this.entrypoint = entrypoint;
        this.objectMapper = objectMapper;
    }

    @Override
    public BrowserResult inspect(BrowserRequest request) {
        try {
            Files.createDirectories(request.projectFolder());
            Process process = new ProcessBuilder(
                    pythonPath,
                    entrypoint,
                    "--url", request.url(),
                    "--output", request.projectFolder().toString()
            ).redirectErrorStream(true).start();
            String output = new String(process.getInputStream().readAllBytes());
            int exit = process.waitFor();
            if (exit != 0) {
                return fallback(request, "browser worker exited " + exit + ": " + output);
            }
            return objectMapper.readValue(output, BrowserResult.class);
        } catch (IOException ex) {
            return fallback(request, ex.getMessage());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return fallback(request, ex.getMessage());
        }
    }

    private BrowserResult fallback(BrowserRequest request, String error) {
        return new BrowserResult(request.url(), "", "", List.of(), List.of(), Map.of(), true, error);
    }
}
