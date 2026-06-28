package com.atlas.career.service;

import com.atlas.career.domain.CompanyRecord;
import com.atlas.career.domain.VisaAssessment;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
public class VisaIntelligenceService {
    private static final List<String> NEGATIVE_SIGNALS = List.of(
            "no sponsorship",
            "no future sponsorship",
            "unable to sponsor",
            "must already be authorized",
            "unrestricted work authorization",
            "no h1b",
            "no h-1b",
            "no opt",
            "no cpt",
            "current or future sponsorship unavailable",
            "without sponsorship now or in the future"
    );

    private static final List<String> POSITIVE_SIGNALS = List.of(
            "sponsorship available",
            "visa sponsorship",
            "h1b sponsorship",
            "h-1b sponsorship",
            "will sponsor"
    );

    public VisaAssessment assess(String description, CompanyRecord company) {
        String text = description == null ? "" : description.toLowerCase(Locale.ROOT);
        List<String> signals = new ArrayList<>();
        NEGATIVE_SIGNALS.stream().filter(text::contains).forEach(signals::add);
        POSITIVE_SIGNALS.stream().filter(text::contains).forEach(signals::add);

        int score = 55;
        int confidence = 45;
        String recommendation = "Review manually";
        String reason = "No strong visa signal found in the job description.";

        if (company != null && company.visaConfidence() > 0) {
            score = Math.max(20, Math.min(85, company.visaConfidence()));
            confidence = Math.max(confidence, 55);
            reason = "Using company visa history because the job description is not explicit.";
        }
        if (signals.stream().anyMatch(NEGATIVE_SIGNALS::contains)) {
            score = 10;
            confidence = 90;
            recommendation = "Skip by default";
            reason = "Job description contains sponsorship restriction language.";
        } else if (signals.stream().anyMatch(POSITIVE_SIGNALS::contains)) {
            score = 90;
            confidence = 80;
            recommendation = "Eligible candidate";
            reason = "Job description contains positive sponsorship language.";
        }

        return new VisaAssessment(score, confidence, recommendation, reason, signals);
    }
}
