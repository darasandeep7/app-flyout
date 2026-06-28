package com.atlas.storage;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CareerMigrationScriptTest {
    @Test
    void migrationContainsCareerIntelligenceTables() throws IOException {
        try (var stream = getClass().getResourceAsStream("/db/migration/V2__career_intelligence.sql")) {
            assertThat(stream).isNotNull();
            String sql = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            assertThat(sql).contains("career_companies", "career_jobs", "career_application_packages", "career_schema_version");
        }
    }
}
