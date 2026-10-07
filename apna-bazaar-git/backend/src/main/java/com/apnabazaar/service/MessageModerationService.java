package com.apnabazaar.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import java.util.Map;
import java.util.List;
import java.text.Normalizer;
import java.util.regex.Pattern;

@Service
public class MessageModerationService {
    public enum Decision { ALLOW, BLOCK, UNAVAILABLE }
    public static final String BLOCK_REPLY = "Let’s keep it respectful. I can help you find food, classes or services in your hood.";
    private static final Pattern PROFANITY = Pattern.compile("(?iu)\\b(?:fuck(?:ing|er|ers|ed)?|motherfuck(?:er|ers|ing)?|cunt|asshole)\\b");
    private static final List<String> BLOCKED = List.of("sexual", "sexual/minors", "harassment", "harassment/threatening", "hate", "hate/threatening", "violence/graphic");
    private final RestClient client;

    @Autowired
    public MessageModerationService(@Value("${spring.ai.openai.api-key}") String key,
                                    @Value("${spring.ai.openai.base-url:https://api.openai.com}") String baseUrl) {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(10000);
        client = RestClient.builder().baseUrl(baseUrl).requestFactory(factory)
            .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + key).build();
    }
    public MessageModerationService(RestClient client) { this.client = client; }

    public Decision check(String text) {
        if (PROFANITY.matcher(Normalizer.normalize(text, Normalizer.Form.NFKC)).find()) return Decision.BLOCK;
        try {
            var result = client.post().uri("/v1/moderations")
                .body(Map.of("model", "omni-moderation-latest", "input", text))
                .retrieve().body(JsonNode.class);
            if (result == null || !result.path("results").isArray() || result.path("results").isEmpty()) return Decision.UNAVAILABLE;
            var categories = result.path("results").get(0).path("categories");
            if (!categories.isObject()) return Decision.UNAVAILABLE;
            for (String category : BLOCKED) {
                if (!categories.path(category).isBoolean()) return Decision.UNAVAILABLE;
                if (categories.path(category).asBoolean()) return Decision.BLOCK;
            }
            return Decision.ALLOW;
        } catch (Exception ignored) {
            // Do not log/store the raw input or credentials; unavailable screening fails closed.
            return Decision.UNAVAILABLE;
        }
    }
}
