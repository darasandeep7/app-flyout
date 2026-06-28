package com.atlas.core.playground;

import com.atlas.ai.ChatRequest;
import com.atlas.ai.ChatResponse;
import com.atlas.ai.ModelProvider;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/playground")
public class PlaygroundController {
    private final ModelProvider modelProvider;

    public PlaygroundController(ModelProvider modelProvider) {
        this.modelProvider = modelProvider;
    }

    @PostMapping("/chat")
    public ChatResponse chat(@RequestBody ChatRequest request) {
        return modelProvider.generate(request);
    }
}
