package br.com.fiap.authlambda.support;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

public final class JsonSupport {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .setPropertyNamingStrategy(PropertyNamingStrategies.LOWER_CAMEL_CASE)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private JsonSupport() {
    }

    public static ObjectMapper mapper() {
        return OBJECT_MAPPER;
    }

    public static <T> T readValue(String json, Class<T> type) {
        try {
            return OBJECT_MAPPER.readValue(json, type);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("JSON invalido", ex);
        }
    }

    public static String writeValueAsString(Object value) {
        try {
            return OBJECT_MAPPER.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Nao foi possivel serializar JSON", ex);
        }
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> asMap(Object value) {
        return OBJECT_MAPPER.convertValue(value, Map.class);
    }

    public static String decodeBody(Object body, boolean base64Encoded) {
        if (body == null) {
            return null;
        }
        String raw = body.toString();
        if (!base64Encoded) {
            return raw;
        }
        return new String(Base64.getDecoder().decode(raw), StandardCharsets.UTF_8);
    }
}

