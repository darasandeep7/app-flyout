package com.atlas.visa;

import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LocalVisaIntelligenceAnalyzerTest {
    private final LocalVisaIntelligenceAnalyzer analyzer = new LocalVisaIntelligenceAnalyzer();

    @Test
    void flagsHardSponsorshipRestrictions() {
        VisaAnalysisResult result = analyzer.analyze(new VisaAnalysisRequest(
                "Example",
                "Engineer",
                "Candidates must already be authorized. No H1B sponsorship is available.",
                0,
                List.of()
        ));

        assertThat(result.visaEligible()).isFalse();
        assertThat(result.visaConfidence()).isGreaterThanOrEqualTo(90);
        assertThat(result.recommendation()).isEqualTo("Visa Risk");
        assertThat(result.supportingEvidence()).isNotEmpty();
    }
}
