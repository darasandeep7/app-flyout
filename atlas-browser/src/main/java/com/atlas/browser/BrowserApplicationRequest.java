package com.atlas.browser;

import java.nio.file.Path;
import java.util.List;

public record BrowserApplicationRequest(
        String url,
        Path outputFolder,
        Path resumePath,
        Path coverLetterPath,
        List<QuestionAnswer> answers
) {
    public record QuestionAnswer(String question, String answer) {
    }
}
