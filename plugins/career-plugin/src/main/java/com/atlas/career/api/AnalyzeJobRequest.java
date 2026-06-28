package com.atlas.career.api;

public record AnalyzeJobRequest(
        String company,
        String companyId,
        String title,
        String location,
        String url,
        String description,
        String salary
) {
}
