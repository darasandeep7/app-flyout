package com.atlas.resume;

import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ResumeIntelligenceServiceTest {
    private final ResumeIntelligenceService service = new ResumeIntelligenceService();

    @Test
    void reportsMissingKeywordsWithoutInventingExperience() {
        ResumeHealth health = service.health(
                new ResumeProfile("Built Java services.", List.of(), List.of("Java", "Spring", "Snowflake"), List.of("master")),
                "Java Spring Snowflake backend"
        );

        assertThat(health.strengths()).contains("Java");
        assertThat(health.missingKeywords()).contains("Spring", "Snowflake");
        assertThat(health.safetyPolicy()).contains("Never fabricate");
    }
}
