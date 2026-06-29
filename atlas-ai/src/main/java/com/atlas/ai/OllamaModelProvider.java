package com.atlas.ai;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class OllamaModelProvider implements ModelProvider, ModelCatalog {
    private final RestClient restClient;
    private final String defaultModel;
    private final AiModelSettingsService modelSettings;
    private List<LocalModel> cachedModels = List.of();
    private Instant cachedAt = Instant.EPOCH;

    public OllamaModelProvider(
            @Value("${atlas.ollama.base-url:http://localhost:11434}") String baseUrl,
            @Value("${atlas.ollama.default-text-model:}") String defaultModel,
            AiModelSettingsService modelSettings
    ) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
        this.defaultModel = defaultModel;
        this.modelSettings = modelSettings;
    }

    @Override
    public synchronized List<LocalModel> availableModels() {
        if (Duration.between(cachedAt, Instant.now()).toSeconds() < 15) {
            return cachedModels;
        }
        try {
            OllamaTagsResponse response = restClient.get()
                    .uri("/api/tags")
                    .retrieve()
                    .body(OllamaTagsResponse.class);
            if (response == null || response.models() == null) {
                return List.of();
            }
            cachedModels = response.models().stream()
                    .map(model -> new LocalModel(model.name(), model.details() == null ? "unknown" : model.details().family(), model.size(), model.modified_at()))
                    .toList();
            cachedAt = Instant.now();
            return cachedModels;
        } catch (RuntimeException ex) {
            return List.of();
        }
    }

    @Override
    public ChatResponse generate(ChatRequest request) {
        Instant started = Instant.now();
        String model = chooseModel(request.model(), request.task());
        if (model.isBlank()) {
            return new ChatResponse("", "No Ollama model is configured or detected.", Duration.between(started, Instant.now()), Map.of(), true, "missing-model");
        }
        try {
            Map<String, Object> payload = Map.of(
                    "model", model,
                    "prompt", request.prompt(),
                    "stream", false,
                    "options", request.options() == null ? Map.of() : request.options()
            );
            Map<?, ?> raw = restClient.post()
                    .uri("/api/generate")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .body(Map.class);
            String text = raw == null || !raw.containsKey("response") ? "" : String.valueOf(raw.get("response"));
            return new ChatResponse(model, text, Duration.between(started, Instant.now()), normalizeRaw(raw), false, null);
        } catch (RuntimeException ex) {
            return new ChatResponse(model, "Ollama is not reachable. Start Ollama and retry.", Duration.between(started, Instant.now()), Map.of(), true, ex.getMessage());
        }
    }

    private String chooseModel(String requested, String task) {
        if (requested != null && !requested.isBlank()) {
            return requested;
        }
        List<String> installed = availableModels().stream().map(LocalModel::name).toList();
        String taskModel = modelSettings.modelFor(task);
        if (installed.contains(taskModel)) {
            return taskModel;
        }
        String fallback = modelSettings.preferences().fastFallback();
        if (installed.contains(fallback)) {
            return fallback;
        }
        if (defaultModel != null && !defaultModel.isBlank()) {
            return defaultModel;
        }
        return installed.stream().findFirst().orElse(taskModel == null ? "" : taskModel);
    }

    private Map<String, Object> normalizeRaw(Map<?, ?> raw) {
        if (raw == null) {
            return Map.of();
        }
        Map<String, Object> normalized = new LinkedHashMap<>();
        raw.forEach((key, value) -> normalized.put(String.valueOf(key), value));
        return normalized;
    }

    private record OllamaTagsResponse(List<OllamaModel> models) {
    }

    private record OllamaModel(String name, String modified_at, long size, OllamaDetails details) {
    }

    private record OllamaDetails(String family) {
    }
}
