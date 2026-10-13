package com.bloodconnect.ai;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class OpenRouterClient {

    private final RestClient restClient;
    private final String apiKey;
    private final List<String> models;

    public OpenRouterClient(
            @Value("${bloodconnect.ai.openrouter.api-key:}") String apiKey,
            @Value("${bloodconnect.ai.openrouter.model:google/gemma-4-31b-it:free,google/gemma-4-26b-a4b-it:free,nvidia/nemotron-3-super-120b-a12b:free}") String model,
            @Value("${bloodconnect.ai.openrouter.base-url:https://openrouter.ai/api/v1}") String baseUrl,
            @Value("${bloodconnect.ai.openrouter.timeout:PT20S}") Duration timeout
    ) {
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.models = Arrays.stream(model == null ? new String[0] : model.split(","))
                .map(String::trim)
                .filter(name -> !name.isEmpty())
                .toList();
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(5));
        requestFactory.setReadTimeout(timeout);
        this.restClient = RestClient.builder().baseUrl(baseUrl).requestFactory(requestFactory).build();
    }

    public boolean isEnabled() {
        return !apiKey.isEmpty() && !models.isEmpty();
    }

    public String model() {
        return models.isEmpty() ? "" : models.get(0);
    }

    public String ask(String systemPrompt, String userPrompt, double temperature) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model());
        if (models.size() > 1) {
            body.put("models", models);
        }
        body.put("temperature", temperature);
        body.put("messages", List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userPrompt)
        ));
        JsonNode response = restClient.post()
                .uri("/chat/completions")
                .header("Authorization", "Bearer " + apiKey)
                .header("X-Title", "BloodConnect")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(JsonNode.class);
        String content = response == null ? "" : response.path("choices").path(0).path("message").path("content").asText("");
        if (content.isBlank()) {
            throw new IllegalStateException("Model reply was empty");
        }
        return content;
    }
}
