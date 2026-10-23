package com.travelplanner.agent.llm;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.core.JsonValue;
import com.anthropic.errors.AnthropicException;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.OutputConfig;
import com.anthropic.models.messages.StopReason;
import com.anthropic.models.messages.StructuredMessage;
import com.anthropic.models.messages.StructuredMessageCreateParams;

/**
 * Thin wrapper around the Anthropic Java SDK. Every method returns empty when
 * Claude is disabled, refuses, or fails, so callers can fall back to rules.
 */
@Service
public class ClaudeService {

    private static final Logger log = LoggerFactory.getLogger(ClaudeService.class);
    private static final String FALLBACK_BETA = "server-side-fallback-2026-07-01";

    private final ClaudeProperties props;
    private final AnthropicClient client;

    public ClaudeService(ClaudeProperties props) {
        this.props = props;
        this.client = props.enabled() ? AnthropicOkHttpClient.builder().apiKey(props.apiKey()).build() : null;
        log.info(props.enabled() ? "Claude enabled with model {}" : "Claude disabled; agents run offline",
                props.model());
    }

    public boolean enabled() {
        return client != null;
    }

    public Optional<String> text(String prompt) {
        if (client == null) {
            return Optional.empty();
        }
        try {
            Message response = client.messages().create(baseParams(prompt).build());
            if (response.stopReason().filter(StopReason.REFUSAL::equals).isPresent()) {
                return Optional.empty();
            }
            String text = response.content().stream()
                    .flatMap(block -> block.text().stream())
                    .map(t -> t.text())
                    .reduce("", String::concat)
                    .strip();
            return text.isEmpty() ? Optional.empty() : Optional.of(text);
        } catch (AnthropicException e) {
            log.warn("Claude text request failed: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /** Asks Claude for JSON matching {@code type}'s schema (structured outputs). */
    public <T> Optional<T> structured(String prompt, Class<T> type) {
        if (client == null) {
            return Optional.empty();
        }
        try {
            StructuredMessageCreateParams<T> params = baseParams(prompt).outputConfig(type).build();
            StructuredMessage<T> response = client.messages().create(params);
            if (response.stopReason().filter(StopReason.REFUSAL::equals).isPresent()) {
                return Optional.empty();
            }
            return response.content().stream()
                    .flatMap(block -> block.text().stream())
                    .map(t -> t.text())
                    .findFirst();
        } catch (AnthropicException e) {
            log.warn("Claude structured request failed: {}", e.getMessage());
            return Optional.empty();
        }
    }

    private MessageCreateParams.Builder baseParams(String prompt) {
        return MessageCreateParams.builder()
                .model(props.model())
                .maxTokens(16000L)
                .outputConfig(OutputConfig.builder().effort(OutputConfig.Effort.of(props.effort())).build())
                // Re-run safety-classifier refusals on Anthropic's recommended fallback model.
                .putAdditionalHeader("anthropic-beta", FALLBACK_BETA)
                .putAdditionalBodyProperty("fallbacks", JsonValue.from("default"))
                .addUserMessage(prompt);
    }
}
