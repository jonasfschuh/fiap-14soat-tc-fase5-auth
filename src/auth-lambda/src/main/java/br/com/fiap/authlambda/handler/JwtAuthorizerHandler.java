package br.com.fiap.authlambda.handler;

import br.com.fiap.authlambda.config.AppConfig;
import br.com.fiap.authlambda.domain.model.AuthenticatedPrincipal;
import br.com.fiap.authlambda.domain.port.TokenService;
import br.com.fiap.authlambda.infra.jwt.JwtTokenService;
import br.com.fiap.authlambda.support.ApiResponseFactory;
import br.com.fiap.authlambda.support.observability.StructuredLogger;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class JwtAuthorizerHandler implements RequestHandler<Map<String, Object>, Map<String, Object>> {

    private final TokenService tokenService;

    public JwtAuthorizerHandler() {
        AppConfig config = AppConfig.fromEnv();
        this.tokenService = new JwtTokenService(config.jwtSettings(), Clock.systemUTC());
    }

    public JwtAuthorizerHandler(TokenService tokenService) {
        this.tokenService = Objects.requireNonNull(tokenService, "tokenService");
    }

    @Override
    public Map<String, Object> handleRequest(Map<String, Object> event, Context context) {
        long startNanos = System.nanoTime();
        String correlationId = resolveCorrelationId(event, context);
        logStructured(context, "auth_authorizer_request", "received", "ok", correlationId, 0, null,
                "[AWS Lambda] Tentativa de autorizacao recebida", Map.of());

        String token = resolveToken(event);
        if (token == null || token.isBlank()) {
            logStructured(context, "auth_authorizer_request", "denied", "missing_token", correlationId,
                    elapsedMillis(startNanos), null,
                    "[AWS Lambda] Acesso negado: token ausente", Map.of("tokenPresent", false));
            return ApiResponseFactory.authorizerResponse(false, Map.of());
        }

        try {
            AuthenticatedPrincipal principal = tokenService.validateToken(token);
            logStructured(context, "auth_authorizer_request", "allowed", "valid_token", correlationId,
                    elapsedMillis(startNanos), principal.username(),
                    "[AWS Lambda]  Acesso autorizado para usuario " + principal.username(),
                    Map.of("tokenPresent", true));
            return ApiResponseFactory.authorizerResponse(true, buildContext(principal));
        } catch (RuntimeException ex) {
            if (context != null && context.getLogger() != null) {
                context.getLogger().log("[AWS Lambda] Token invalido para authorizer\n");
            }
            logStructured(context, "auth_authorizer_request", "denied", "invalid_token", correlationId,
                    elapsedMillis(startNanos), null,
                    "[AWS Lambda] Acesso negado: token invalido",
                    Map.of(
                            "tokenPresent", true,
                            "errorType", ex.getClass().getSimpleName()
                    ));
            return ApiResponseFactory.authorizerResponse(false, Map.of());
        }
    }

    private long elapsedMillis(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }

    private String resolveCorrelationId(Map<String, Object> event, Context context) {
        if (event != null) {
            Object headersObject = event.get("headers");
            if (headersObject instanceof Map<?, ?> headers) {
                for (Map.Entry<?, ?> entry : headers.entrySet()) {
                    if (entry.getKey() == null || entry.getValue() == null) {
                        continue;
                    }
                    if ("x-correlation-id".equalsIgnoreCase(entry.getKey().toString())) {
                        String value = entry.getValue().toString();
                        if (!value.isBlank()) {
                            return value;
                        }
                    }
                }
            }
        }

        if (context != null && context.getAwsRequestId() != null && !context.getAwsRequestId().isBlank()) {
            return context.getAwsRequestId();
        }
        return UUID.randomUUID().toString();
    }

    @SuppressWarnings("unchecked")
    private String resolveToken(Map<String, Object> event) {
        if (event == null) {
            return null;
        }

        Object headersObject = event.get("headers");
        if (headersObject instanceof Map<?, ?> headers) {
            Object authorization = headers.get("Authorization");
            if (authorization == null) {
                authorization = headers.get("authorization");
            }
            String headerValue = authorization == null ? null : authorization.toString();
            return extractBearerToken(headerValue);
        }

        Object authToken = event.get("authorizationToken");
        if (authToken != null) {
            return extractBearerToken(authToken.toString());
        }

        Object identitySource = event.get("identitySource");
        if (identitySource instanceof Object[] values && values.length > 0) {
            return extractBearerToken(String.valueOf(values[0]));
        }

        return null;
    }

    private String extractBearerToken(String headerValue) {
        if (headerValue == null || headerValue.isBlank()) {
            return null;
        }
        String prefix = "Bearer ";
        if (headerValue.regionMatches(true, 0, prefix, 0, prefix.length())) {
            return headerValue.substring(prefix.length()).trim();
        }
        return headerValue.trim();
    }

    private Map<String, Object> buildContext(AuthenticatedPrincipal principal) {
        Map<String, Object> context = new LinkedHashMap<>();
        context.put("sub", principal.username());
        context.put("username", principal.username());
        context.put("role", principal.role());
        context.put("email", principal.email());
        return context;
    }

    private void logStructured(
            Context context,
            String eventName,
            String stage,
            String reason,
            String correlationId,
            long latencyMs,
            String principal,
            String message,
            Map<String, Object> extras) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("event", eventName);
        payload.put("flow", "authorizer");
        payload.put("stage", stage);
        payload.put("reason", reason);
        payload.put("message", message);
        payload.put("correlationId", correlationId);
        payload.put("latencyMs", latencyMs);
        payload.put("principal", principal);
        payload.put("timestamp", Instant.now().toString());

        if (context != null) {
            payload.put("awsRequestId", context.getAwsRequestId());
            payload.put("functionName", context.getFunctionName());
        }
        if (extras != null && !extras.isEmpty()) {
            payload.putAll(extras);
        }

        String level = "denied".equals(stage) ? "WARN" : "INFO";
        StructuredLogger.emit(context, level, payload);
    }
}

