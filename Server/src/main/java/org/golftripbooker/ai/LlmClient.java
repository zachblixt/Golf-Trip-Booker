package org.golftripbooker.ai;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * The seam. Everything above this line is ordinary Java that can be tested without a
 * network, a key, or a bill; everything below it is one vendor's HTTP API.
 *
 * Deliberately narrow. It knows nothing about golf, proposals or bookings -- it takes
 * two prompts and a JSON Schema and returns an object matching that schema. That is what
 * lets ProposalDraftServiceTest exercise the prompt, the parsing and the validation with
 * a fake that returns canned JSON, which in turn is what lets CI run this feature's tests
 * on every push without calling a paid API.
 */
public interface LlmClient {

    /**
     * False when no API key is configured. Callers use this to degrade politely rather
     * than throwing: the app has to start, and every screen has to work, without a key.
     */
    boolean isConfigured();

    /**
     * @param schemaJson JSON Schema for the object wanted back.
     * @return the model's answer, conforming to that schema.
     */
    JsonNode completeAsJson(String systemPrompt, String userPrompt, String schemaJson)
            throws LlmUnavailableException;
}
