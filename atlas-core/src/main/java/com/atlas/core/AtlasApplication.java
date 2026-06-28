package com.atlas.core;

import com.atlas.core.config.AtlasProperties;
import com.atlas.settings.CareerSettings;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "com.atlas")
@EnableScheduling
@EnableConfigurationProperties({AtlasProperties.class, CareerSettings.class})
public class AtlasApplication {
    public static void main(String[] args) {
        SpringApplication.run(AtlasApplication.class, args);
    }
}
