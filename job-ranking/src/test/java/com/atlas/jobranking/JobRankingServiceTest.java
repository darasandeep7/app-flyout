package com.atlas.jobranking;

import com.atlas.settings.CareerSettings;
import com.atlas.visa.VisaAnalysisResult;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JobRankingServiceTest {
    private final JobRankingService service = new JobRankingService(new CareerSettings(true, List.of("Remote"), List.of("Java"), 70, "0 0 8 * * *"));

    @Test
    void scoresStrongBackendJobsHighly() {
        JobIntelligenceScore score = service.score(new JobIntelligenceRequest(
                "Example",
                "Senior Java Backend Engineer",
                "Remote",
                "$150k",
                "Remote",
                Instant.now(),
                "Java Spring Boot backend APIs microservices Snowflake cloud lead architect",
                List.of("Java", "Spring"),
                "Senior",
                new VisaAnalysisResult(true, 85, 80, "ok", List.of(), "Apply Today", false, true)
        ));

        assertThat(score.overallMatch()).isGreaterThanOrEqualTo(80);
        assertThat(score.javaMatch()).isGreaterThanOrEqualTo(90);
        assertThat(score.explanations()).containsKey("Overall");
    }

    @Test
    void detectsLikelyDuplicates() {
        JobIntelligenceRequest left = request("Senior Java Engineer", "Remote", "Java Spring backend APIs");
        JobIntelligenceRequest right = request("Senior Java Engineer", "Remote", "Java Spring backend APIs");

        DuplicateAssessment duplicate = service.duplicate(left, right);

        assertThat(duplicate.likelyDuplicate()).isTrue();
        assertThat(duplicate.duplicateConfidence()).isGreaterThanOrEqualTo(65);
    }

    private JobIntelligenceRequest request(String title, String location, String description) {
        return new JobIntelligenceRequest("Example", title, location, "", "Remote", Instant.now(), description, List.of("Java"), "Senior", null);
    }
}
