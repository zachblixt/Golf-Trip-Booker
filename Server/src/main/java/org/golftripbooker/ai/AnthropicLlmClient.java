package org.golftripbooker.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * Anthropic's Messages API, over Spring's own RestClient. No new dependency: RestClient
 * and Jackson both arrive with spring-boot-starter-web, which this project already has.
 *
 * Structure comes from tool use rather than from asking for JSON in the prompt. The model
 * is given exactly one tool and told it must call it, so the answer arrives as the tool's
 * typed input instead of prose that has to be scraped for a code fence.
 */
@Component
public class AnthropicLlmClient implements LlmClient {

    private static final String TOOL_NAME = "submit_draft";
    private static final String ANTHROPIC_VERSION = "2023-06-01";

    private final RestClient rest;
    private final ObjectMapper mapper;
    private final String apiKey;
    private final String model;
    private final int maxTokens;

    public AnthropicLlmClient(@Value("${anthropic.api-key:}") String apiKey,
                              @Value("${anthropic.model}") String model,
                              @Value("${anthropic.base-url}") String baseUrl,
                              @Value("${anthropic.max-tokens}") int maxTokens,
                              @Value("${anthropic.timeout-seconds}") int timeoutSeconds,
                              ObjectMapper mapper) {
        this.apiKey = apiKey;
        this.model = model;
        this.maxTokens = maxTokens;
        this.mapper = mapper;

        /*
         * Timeouts are not optional here. A host is waiting on this call inside their own
         * HTTP request, and the default is to wait forever. Better a clean failure and a
         * blank form after a few seconds than a spinner that never resolves.
         */
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(timeoutSeconds));

        this.rest = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .build();
    }

    @Override
    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    @Override
    public JsonNode completeAsJson(String systemPrompt, String userPrompt, String schemaJson)
            throws LlmUnavailableException {

        if (!isConfigured()) {
            throw new LlmUnavailableException("No ANTHROPIC_API_KEY is set.");
        }

        ObjectNode body;
        try {
            body = mapper.createObjectNode();
            body.put("model", model);
            body.put("max_tokens", maxTokens);
            body.put("system", systemPrompt);

            ObjectNode message = body.putArray("messages").addObject();
            message.put("role", "user");
            message.put("content", userPrompt);

            ObjectNode tool = body.putArray("tools").addObject();
            tool.put("name", TOOL_NAME);
            tool.put("description", "Submit the drafted proposal.");
            tool.set("input_schema", mapper.readTree(schemaJson));

            // Not "you may use this tool" but "your answer is a call to this tool".
            ObjectNode choice = body.putObject("tool_choice");
            choice.put("type", "tool");
            choice.put("name", TOOL_NAME);
        } catch (Exception ex) {
            throw new LlmUnavailableException("Could not build the request body.", ex);
        }

        JsonNode response;
        try {
            response = rest.post()
                    .uri("/v1/messages")
                    .header("x-api-key", apiKey)
                    .header("anthropic-version", ANTHROPIC_VERSION)
                    .header("content-type", "application/json")
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);
        } catch (Exception ex) {
            // Timeouts, connection refused, 4xx, 5xx, rate limits. All the same to a caller.
            throw new LlmUnavailableException("The model API call failed: " + ex.getMessage(), ex);
        }

        if (response == null) {
            throw new LlmUnavailableException("The model API returned an empty body.");
        }

        for (JsonNode block : response.path("content")) {
            if ("tool_use".equals(block.path("type").asText())
                    && TOOL_NAME.equals(block.path("name").asText())) {
                return block.path("input");
            }
        }

        /*
         * tool_choice should make this unreachable, but "should" is not a guarantee about
         * somebody else's service. A stop_reason of max_tokens lands here too, which is
         * the realistic way it happens.
         */
        throw new LlmUnavailableException(
                "The model did not call the tool. stop_reason was "
                        + response.path("stop_reason").asText("unknown") + ".");
    }
}
