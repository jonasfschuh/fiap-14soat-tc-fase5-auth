package br.com.fiap.authlambda.support;

import java.util.LinkedHashMap;
import java.util.Map;

public final class ApiResponseFactory {

    private ApiResponseFactory() {
    }

    public static Map<String, Object> json(int statusCode, Object body) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("statusCode", statusCode);
        response.put("headers", headers());
        response.put("body", JsonSupport.writeValueAsString(body));
        response.put("isBase64Encoded", false);
        return response;
    }

    public static Map<String, Object> authorizerResponse(boolean authorized, Map<String, Object> context) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("isAuthorized", authorized);
        if (context != null && !context.isEmpty()) {
            response.put("context", context);
        }
        return response;
    }

    public static Map<String, String> headers() {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("Access-Control-Allow-Origin", "*");
        headers.put("Access-Control-Allow-Headers", "content-type,authorization");
        headers.put("Access-Control-Allow-Methods", "GET,POST,OPTIONS");
        return headers;
    }

    public static Map<String, Object> error(int statusCode, String message) {
        return json(statusCode, Map.of("message", message));
    }
}

