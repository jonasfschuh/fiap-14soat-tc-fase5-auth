package br.com.fiap.authlambda.support.observability;

import br.com.fiap.authlambda.support.JsonSupport;
import com.amazonaws.services.lambda.runtime.Context;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Centralizes structured JSON log emission for New Relic Lambda log ingestion.
 *
 * <p>New Relic Lambda Extension captures stdout and forwards logs to New Relic.
 * Using System.out.println ensures the output is pure JSON (no Lambda runtime
 * prefix), allowing New Relic to parse and index all structured fields.
 * The {@code entity.name} field is required for New Relic to correlate the log
 * to the correct aws-lambda entity in the UI.
 */
public final class StructuredLogger {

    private static final String ENTITY_TYPE = "AWSLAMBDAFUNCTION";

    private StructuredLogger() {
    }

    /**
     * Emits a structured JSON log line to stdout.
     *
     * @param context  Lambda context (may be null in tests)
     * @param level    Log level string: INFO, WARN, ERROR
     * @param payload  Pre-built payload — entity.name and level will be injected
     */
    public static void emit(Context context, String level, Map<String, Object> payload) {
        Map<String, Object> enriched = new LinkedHashMap<>();

        // New Relic entity correlation fields — must be first for readability
        String functionName = resolveFunctionName(context);
        enriched.put("entity.name", functionName);
        enriched.put("entity.type", ENTITY_TYPE);
        enriched.put("level", level == null ? "INFO" : level.toUpperCase());

        enriched.putAll(payload);

        System.out.println(JsonSupport.writeValueAsString(enriched));
    }

    public static void info(Context context, Map<String, Object> payload) {
        emit(context, "INFO", payload);
    }

    public static void warn(Context context, Map<String, Object> payload) {
        emit(context, "WARN", payload);
    }

    public static void error(Context context, Map<String, Object> payload) {
        emit(context, "ERROR", payload);
    }

    private static String resolveFunctionName(Context context) {
        if (context != null && context.getFunctionName() != null && !context.getFunctionName().isBlank()) {
            return context.getFunctionName();
        }
        String envName = System.getenv("AWS_LAMBDA_FUNCTION_NAME");
        return (envName != null && !envName.isBlank()) ? envName : "auth-lambda";
    }
}
