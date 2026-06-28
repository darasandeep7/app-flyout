package com.atlas.visa;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
public class LocalVisaIntelligenceAnalyzer implements VisaIntelligenceAnalyzer {
    private static final List<String> HARD_NEGATIVE = List.of(
            "no sponsorship",
            "no future sponsorship",
            "unable to sponsor",
            "requires permanent authorization",
            "must already be authorized",
            "no h1b",
            "no h-1b",
            "no opt",
            "no cpt",
            "current or future sponsorship not available",
            "requires unrestricted work authorization",
            "without sponsorship now or in the future"
    );

    private static final List<String> SOFT_NEGATIVE = List.of(
            "authorized to work in the united states",
            "employment authorization",
            "work authorization required"
    );

    private static final List<String> POSITIVE = List.of(
            "visa sponsorship available",
            "sponsorship available",
            "h1b sponsorship",
            "h-1b sponsorship",
            "will sponsor",
            "sponsor qualified candidates"
    );

    @Override
    public VisaAnalysisResult analyze(VisaAnalysisRequest request) {
        String text = normalize(request.description());
        List<String> evidence = new ArrayList<>();
        HARD_NEGATIVE.stream().filter(text::contains).forEach(evidence::add);
        if (!evidence.isEmpty()) {
            return new VisaAnalysisResult(false, 10, 92, "Strong sponsorship restriction language was found.", evidence, "Visa Risk", false, true);
        }

        POSITIVE.stream().filter(text::contains).forEach(evidence::add);
        if (!evidence.isEmpty()) {
            return new VisaAnalysisResult(true, 90, 82, "Positive sponsorship language was found.", evidence, "Apply Today", false, true);
        }

        SOFT_NEGATIVE.stream().filter(text::contains).forEach(evidence::add);
        if (!evidence.isEmpty()) {
            return new VisaAnalysisResult(false, 45, 58, "Authorization language may indicate risk but is not definitive.", evidence, "Needs Manual Review", true, true);
        }

        int companyConfidence = Math.max(0, Math.min(100, request.companyVisaConfidence()));
        if (companyConfidence >= 70) {
            return new VisaAnalysisResult(true, companyConfidence, 65, "Company history suggests sponsorship may be possible.", request.companyHistory(), "Good Match", false, true);
        }
        if (companyConfidence > 0 && companyConfidence < 40) {
            return new VisaAnalysisResult(false, companyConfidence, 62, "Company history suggests low sponsorship likelihood.", request.companyHistory(), "Needs Manual Review", true, true);
        }
        return new VisaAnalysisResult(false, 55, 35, "No strong visa signal found. Manual review is recommended.", List.of(), "Needs Manual Review", true, true);
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
    }
}
