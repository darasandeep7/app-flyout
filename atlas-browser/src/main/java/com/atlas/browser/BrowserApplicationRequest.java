package com.atlas.browser;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public record BrowserApplicationRequest(
        String url,
        Path outputFolder,
        Path resumePath,
        Path coverLetterPath,
        List<QuestionAnswer> answers,
        Map<String, String> userProfile
) {
    public record QuestionAnswer(String question, String answer) {
    }
}
