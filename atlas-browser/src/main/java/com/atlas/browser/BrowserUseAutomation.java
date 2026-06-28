package com.atlas.browser;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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

    @Override
    public BrowserApplicationResult apply(BrowserApplicationRequest request) {
        try {
            Files.createDirectories(request.outputFolder());
            Path payload = request.outputFolder().resolve("browser-application-payload.json");
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(payload.toFile(), request);
            Process process = new ProcessBuilder(
                    pythonPath,
                    entrypoint,
                    "--application", payload.toString()
            ).redirectErrorStream(true).start();
            String output = new String(process.getInputStream().readAllBytes());
            int exit = process.waitFor();
            if (exit != 0) {
                return browserFallback("browser worker exited " + exit + ": " + output);
            }
            return objectMapper.readValue(output, BrowserApplicationResult.class);
        } catch (IOException ex) {
            return browserFallback(ex.getMessage());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return browserFallback(ex.getMessage());
        }
    }

    private BrowserResult fallback(BrowserRequest request, String error) {
        return new BrowserResult(request.url(), "", "", List.of(), List.of(), Map.of(), true, error);
    }

    private BrowserApplicationResult browserFallback(String error) {
        return new BrowserApplicationResult("PAUSED_FOR_MANUAL_REVIEW", "Browser automation unavailable", List.of(), List.of(), true, error);
    }
}
