package br.com.fiap.authlambda.support.observability;

import com.amazonaws.services.lambda.runtime.Context;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CloudWatchAuthTelemetry implements AuthTelemetry {

    private static final String METRIC_NAMESPACE = "Raceforce/Auth";

    @Override
    public void recordLoginSuccess(Context context, String cpf, String correlationId, long latencyMs) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("event", "auth_login");
        payload.put("authResult", "SUCCESS");
        payload.put("reason", "ok");
        payload.put("message", "Usuario autenticado com sucesso");
        payload.put("cpf", cpf);
        payload.put("latencyMs", latencyMs);
        payload.put("correlationId", correlationId);
        payload.put("timestamp", Instant.now().toString());
        logJson(context, payload);

        emitMetric(context, "auth_login_success_total", null);
    }

    @Override
    public void recordLoginFailure(Context context, String cpf, String correlationId, String reason, long latencyMs) {
        String normalizedReason = normalizeReason(reason);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("event", "auth_login");
        payload.put("authResult", "FAILURE");
        payload.put("reason", normalizedReason);
        payload.put("message", "Tentativa de login negada");
        payload.put("cpf", cpf);
        payload.put("latencyMs", latencyMs);
        payload.put("correlationId", correlationId);
        payload.put("timestamp", Instant.now().toString());
        logJson(context, payload);

        emitMetric(context, "auth_login_failure_total", normalizedReason);
    }

    @Override
    public void recordTokenIssued(Context context, String cpf, String correlationId) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("event", "auth_token_issued");
        payload.put("authResult", "SUCCESS");
        payload.put("reason", "token_issued");
        payload.put("message", "Token JWT emitido com sucesso");
        payload.put("cpf", cpf);
        payload.put("correlationId", correlationId);
        payload.put("timestamp", Instant.now().toString());
        logJson(context, payload);

        emitMetric(context, "auth_token_issued_total", null);
    }

    private void emitMetric(Context context, String metricName, String reason) {
        Map<String, Object> emf = new LinkedHashMap<>();
        String functionName = System.getenv().getOrDefault("AWS_LAMBDA_FUNCTION_NAME", "auth-lambda");

        Map<String, Object> awsMeta = new LinkedHashMap<>();
        awsMeta.put("Timestamp", Instant.now().toEpochMilli());

        Map<String, Object> metricDefinition = new LinkedHashMap<>();
        metricDefinition.put("Namespace", METRIC_NAMESPACE);
        metricDefinition.put("Dimensions", reason == null ? List.of(List.of("function")) : List.of(List.of("function", "reason")));
        metricDefinition.put("Metrics", List.of(Map.of("Name", metricName, "Unit", "Count")));

        awsMeta.put("CloudWatchMetrics", List.of(metricDefinition));

        emf.put("_aws", awsMeta);
        emf.put("function", functionName);
        if (reason != null) {
            emf.put("reason", reason);
        }
        emf.put(metricName, 1);

        logJson(context, emf);
    }

    private String normalizeReason(String reason) {
        if (reason == null || reason.isBlank()) {
            return "unknown";
        }
        return reason.trim().toLowerCase().replace(' ', '_');
    }

    private void logJson(Context context, Map<String, Object> payload) {
        StructuredLogger.info(context, payload);
    }
}

