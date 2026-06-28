package com.atlas.property;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/plugins/property")
public class PropertyController {
    private final PropertyWorkflow workflow;

    public PropertyController(PropertyWorkflow workflow) {
        this.workflow = workflow;
    }

    @PostMapping("/analyze")
    public PropertyAnalysisResult analyze(@RequestBody PropertyAnalysisRequest request) {
        return workflow.analyze(request);
    }
}
