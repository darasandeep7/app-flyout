package com.atlas.core.agents;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BuiltInAgents {
    @Bean
    Agent managerAgent() {
        return simple("manager", "Manager Agent");
    }

    @Bean
    Agent browserAgent() {
        return simple("browser", "Browser Agent");
    }

    @Bean
    Agent visionAgent() {
        return simple("vision", "Vision Agent");
    }

    @Bean
    Agent marketingAgent() {
        return simple("marketing", "Marketing Agent");
    }

    @Bean
    Agent writerAgent() {
        return simple("writer", "Writer Agent");
    }

    @Bean
    Agent videoPlannerAgent() {
        return simple("video-planner", "Video Planner Agent");
    }

    @Bean
    Agent memoryAgent() {
        return simple("memory", "Memory Agent");
    }

    private Agent simple(String id, String name) {
        return new Agent() {
            @Override
            public String id() {
                return id;
            }

            @Override
            public String name() {
                return name;
            }

            @Override
            public AgentResult run(AgentTask task) {
                Instant started = Instant.now();
                return new AgentResult(id, name + " accepted task: " + task.objective(), Map.of("context", task.context()), Duration.between(started, Instant.now()), true);
            }
        };
    }
}
