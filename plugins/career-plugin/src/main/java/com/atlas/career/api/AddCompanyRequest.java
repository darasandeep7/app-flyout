package com.atlas.career.api;

import java.util.List;

public record AddCompanyRequest(String name, String careerUrl, List<String> locations, int priority, String notes) {
}
