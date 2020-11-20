package org.sysethereum.agents.service.rest;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpsExchange;
import org.springframework.lang.NonNull;
import org.springframework.util.StringUtils;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.OutputStream;
import java.util.LinkedHashMap;
import java.util.Map;

public abstract class CommonHttpHandler implements HttpHandler {

    @Nullable
    protected String sanitizeHash(@Nullable String hash) {
        if (hash != null && hash.startsWith("0x"))
            hash = hash.substring(2);

        return hash;
    }

    /**
     * @param httpsExchange
     * @return is HTTP request handled?
     */
    protected boolean setOriginAndHandleOptionsMethod(HttpsExchange httpsExchange) throws IOException {
        httpsExchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");

        if (httpsExchange.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
            httpsExchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, OPTIONS");
            httpsExchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type,Authorization");
            httpsExchange.sendResponseHeaders(204, -1);
            return true;
        }

        return false;
    }

    /**
     * Obtain all params of current request.
     *
     * @return params
     */
    @NonNull
    protected Map<String, Object> getParams(@NonNull HttpsExchange exchange) {
        Map<String, Object> params = new LinkedHashMap<>();

        if (!StringUtils.isEmpty(exchange.getRequestURI().getQuery())) {
            Map<String, String> queryParams = queryToMap(exchange.getRequestURI().getQuery());
            if (queryParams != null) {
                params.putAll(queryParams);
            }
        }

        return params;
    }

    /**
     * returns the url parameters in a map preserving order of insertion
     *
     * @param query
     * @return map
     */
    protected LinkedHashMap<String, String> queryToMap(String query) {
        LinkedHashMap<String, String> result = new LinkedHashMap<>();
        for (String param : query.split("&")) {
            String[] pair = param.split("=");
            if (pair.length > 1) {
                result.put(pair[0], pair[1]);
            } else {
                result.put(pair[0], "");
            }
        }
        return result;
    }

    protected void writeResponse(HttpsExchange httpsExchange, String response) throws IOException {
        httpsExchange.sendResponseHeaders(200, response.length());
        OutputStream os = httpsExchange.getResponseBody();
        os.write(response.getBytes());
        os.close();
    }

}
