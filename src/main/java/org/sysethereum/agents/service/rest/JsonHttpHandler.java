package org.sysethereum.agents.service.rest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpsExchange;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Extends `Json response`.
 *
 * @author Jover Zhang
 */
public abstract class JsonHttpHandler extends CommonHttpHandler {

    protected final String APPLICATION_JSON = "application/json";

    private static final String CONTENT_TYPE = "Content-Type";

    private final ObjectMapper objectMapper = new ObjectMapper();


    @NonNull
    @Override
    protected Map<String, Object> getParams(@NonNull HttpsExchange exchange) {
        Map<String, Object> params = super.getParams(exchange);

        if (isMatchedApplicationJson(exchange.getRequestHeaders())) {
            Map<String, Object> jsonParams = parseRequestBodyByJson(exchange.getRequestBody());
            if (jsonParams != null) {
                params.putAll(jsonParams);
            }
        }

        return params;
    }

    /**
     * Verify the `ContentType: application/json` appear in `RequestHeader`.
     */
    protected boolean isMatchedApplicationJson(@NonNull Headers requestHeaders) {
        return requestHeaders.get(CONTENT_TYPE).stream()
                .anyMatch(APPLICATION_JSON::equalsIgnoreCase);
    }

    @Nullable
    @SuppressWarnings("unchecked")
    protected Map<String, Object> parseRequestBodyByJson(@NonNull InputStream requestBody) {
        try (requestBody) {
            byte[] bytes = new byte[requestBody.available()];
            if (requestBody.read(bytes) <= 0) {
                return null;
            }

            String jsonStringOfParams = new String(bytes, Charset.defaultCharset());
            return objectMapper.readValue(jsonStringOfParams, LinkedHashMap.class);
        } catch (IOException e) {
            return null;
        }
    }

    protected void writeJsonResponse(HttpsExchange exchange, List<Object> response) throws IOException {
        doWriteJsonResponse(exchange, response);
    }

    protected void writeJsonResponse(HttpsExchange exchange, Map<String, Object> response) throws IOException {
        doWriteJsonResponse(exchange, response);
    }

    private <T> void doWriteJsonResponse(HttpsExchange exchange, T response) throws IOException {
        String result;
        try {
            result = objectMapper.writeValueAsString(response);
        } catch (JsonProcessingException e) {
            throw new IOException("Object: " + response + " cannot be converted to JSON.");
        }
        writeResponse(exchange, result);
    }

}
