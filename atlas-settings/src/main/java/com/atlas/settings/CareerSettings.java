package com.atlas.settings;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "atlas.career")
public record CareerSettings(
        boolean visaRequired,
        List<String> preferredLocations,
        List<String> targetSkills,
        int minimumMatchScore,
        String dailyScanCron
) {
}
