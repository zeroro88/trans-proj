package com.transproj.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.transproj.config.AppProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** vLLM / OpenAI-compatible {@code POST .../chat/completions}. */
@Component
public class OpenAiCompatibleLlmClient {

    private static final Duration DEFAULT_TIMEOUT = Duration.ofMinutes(10);

    private final AppProperties appProperties;
    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public OpenAiCompatibleLlmClient(AppProperties appProperties, WebClient.Builder webClientBuilder, ObjectMapper objectMapper) {
        this.appProperties = appProperties;
        this.webClient = webClientBuilder.build();
        this.objectMapper = objectMapper;
    }

    public String chat(String systemPrompt, String userMessage) throws LlmException {
        String base = appProperties.getLlm().getBaseUrl().replaceAll("/$", "");
        String url = base + "/chat/completions";

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", appProperties.getLlm().getModel());
        body.put("temperature", 0.2);
        body.put("messages", List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userMessage)
        ));

        try {
            WebClient.RequestBodySpec spec = webClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON);
            String apiKey = appProperties.getLlm().getApiKey();
            if (apiKey != null && !apiKey.isBlank()) {
                spec = spec.header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey.trim());
            }
            String raw = spec.bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(DEFAULT_TIMEOUT);

            if (raw == null || raw.isBlank()) {
                throw new LlmException("LLM_EMPTY", "Empty completion response");
            }
            return parseChoiceText(raw);
        } catch (WebClientResponseException e) {
            throw new LlmException("LLM_HTTP_" + e.getStatusCode().value(), e.getResponseBodyAsString(), e);
        } catch (LlmException e) {
            throw e;
        } catch (Exception e) {
            throw new LlmException("LLM_FAILED", e.getMessage(), e);
        }
    }

    private String parseChoiceText(String raw) throws LlmException {
        try {
            JsonNode root = objectMapper.readTree(raw);
            JsonNode choices = root.path("choices");
            if (!choices.isArray() || choices.isEmpty()) {
                throw new LlmException("LLM_BAD_OUTPUT", "No choices in completion response");
            }
            JsonNode message = choices.get(0).path("message");
            String content = message.path("content").asText(null);
            if (content == null || content.isBlank()) {
                throw new LlmException("LLM_BAD_OUTPUT", "Missing message.content");
            }
            return content;
        } catch (LlmException e) {
            throw e;
        } catch (Exception e) {
            throw new LlmException("LLM_BAD_OUTPUT", "Invalid completion JSON: " + e.getMessage(), e);
        }
    }
}
