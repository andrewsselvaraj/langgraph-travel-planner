package com.travelplanner.agent.llm;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Claude settings. When {@code apiKey} is blank the agents run offline with
 * rule-based fallbacks.
 */
@ConfigurationProperties(prefix = "travel-planner.claude")
public record ClaudeProperties(String apiKey, String model, String effort) {

    public ClaudeProperties {
        model = model == null || model.isBlank() ? "claude-opus-5-5" : model;
        effort = effort == null || effort.isBlank() ? "medium" : effort;
    }

    public boolean enabled() {
        return apiKey != null && !apiKey.isBlank();
    }
}
