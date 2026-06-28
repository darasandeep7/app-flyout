package com.atlas.jobranking;

import java.util.List;

public record DuplicateAssessment(boolean likelyDuplicate, int duplicateConfidence, List<String> matchingSignals) {
}
