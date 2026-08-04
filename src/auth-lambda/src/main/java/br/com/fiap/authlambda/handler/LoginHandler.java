package br.com.fiap.authlambda.handler;

import br.com.fiap.authlambda.config.AppConfig;
import br.com.fiap.authlambda.domain.model.LoginRequest;
import br.com.fiap.authlambda.domain.model.LoginResponse;
import br.com.fiap.authlambda.domain.port.AuthenticationUseCase;
import br.com.fiap.authlambda.domain.service.AuthenticationService;
import br.com.fiap.authlambda.domain.service.UnauthorizedException;
import br.com.fiap.authlambda.infra.crypto.BCryptPasswordVerifier;
import br.com.fiap.authlambda.infra.jwt.JwtTokenService;
import br.com.fiap.authlambda.infra.repository.HikariDataSourceFactory;
import br.com.fiap.authlambda.infra.repository.PostgresUserRepository;
import br.com.fiap.authlambda.support.observability.AuthTelemetry;
import br.com.fiap.authlambda.support.observability.CloudWatchAuthTelemetry;
import br.com.fiap.authlambda.support.observability.StructuredLogger;
import br.com.fiap.authlambda.support.ApiResponseFactory;
import br.com.fiap.authlambda.support.JsonSupport;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

import javax.sql.DataSource;
import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class LoginHandler implements RequestHandler<Map<String, Object>, Map<String, Object>> {

    private final AuthenticationUseCase authenticationService;
    private final AuthTelemetry authTelemetry;
    private final String initializationError;

    public LoginHandler() {
        AuthenticationUseCase initializedService = null;
        String initError = null;
        try {
            AppConfig config = AppConfig.fromEnv();
            DataSource dataSource = HikariDataSourceFactory.create(config.dbSettings());
            initializedService = new AuthenticationService(
                    new PostgresUserRepository(dataSource),
                    new BCryptPasswordVerifier(),
                    new JwtTokenService(config.jwtSettings(), Clock.systemUTC()),
                    Clock.systemUTC(),
                    config.jwtSettings()
            );
        } catch (Exception ex) {
            initError = ex.getMessage() == null ? "falha de inicializacao" : ex.getMessage();
        }
        this.authenticationService = initializedService;
        this.authTelemetry = new CloudWatchAuthTelemetry();
        this.initializationError = initError;
    }

    public LoginHandler(AuthenticationUseCase authenticationService) {
        this(authenticationService, new CloudWatchAuthTelemetry());
    }

    public LoginHandler(AuthenticationUseCase authenticationService, AuthTelemetry authTelemetry) {
        this.authenticationService = authenticationService;
        this.authTelemetry = authTelemetry;
        this.initializationError = null;
    }

    @Override
    public Map<String, Object> handleRequest(Map<String, Object> event, Context context) {
        long startNanos = System.nanoTime();
        String correlationId = resolveCorrelationId(event, context);
        logStructured(context, "auth_login_request", "received", "ok", correlationId, 0, null, null,
                "Tentativa de login recebida", Map.of(
                "hasBody", event != null && event.get("body") != null,
                "requestSource", "api_gateway"
        ));
        LoginRequest request = null;
        try {
            if (authenticationService == null) {
                String message = "Erro interno na inicializacao da lambda: " + initializationError;
                log(context, message);
                logStructured(context, "auth_login_request", "failed", "initialization_error", correlationId,
                        elapsedMillis(startNanos), null, null,
                        "Falha na inicializacao da lambda de login", Map.of());
                authTelemetry.recordLoginFailure(context, null, correlationId, "initialization_error", elapsedMillis(startNanos));
                return ApiResponseFactory.error(500, message);
            }

            request = parseRequest(event);
            String username = safeUsername(request);
            logStructured(context, "auth_login_request", "parsed", "ok", correlationId,
                    elapsedMillis(startNanos), safeCpf(request), username,
                    "Tentativa de login usuario " + username, Map.of(
                            "usernamePresent", request.username() != null && !request.username().isBlank()
                    ));
            LoginResponse response = authenticationService.authenticate(request);
            String cpf = extractCpf(request.username());
            long latencyMs = elapsedMillis(startNanos);
            logStructured(context, "auth_login_request", "authenticated", "ok", correlationId, latencyMs, cpf,
                    username, "Usuario " + username + " autenticado com sucesso", Map.of());
            authTelemetry.recordLoginSuccess(context, cpf, correlationId, latencyMs);
            authTelemetry.recordTokenIssued(context, cpf, correlationId);
            return ApiResponseFactory.json(200, response);
        } catch (IllegalArgumentException ex) {
            log(context, ex.getMessage());
            String username = safeUsername(request);
            logStructured(context, "auth_login_request", "failed", "invalid_request", correlationId,
                    elapsedMillis(startNanos), safeCpf(request), username,
                    "Falha de validacao no login para usuario " + username, Map.of("errorMessage", ex.getMessage()));
            authTelemetry.recordLoginFailure(context, safeCpf(request), correlationId, "invalid_request", elapsedMillis(startNanos));
            return ApiResponseFactory.error(400, ex.getMessage());
        } catch (UnauthorizedException ex) {
            log(context, ex.getMessage());
            String reason = classifyUnauthorizedReason(ex.getMessage());
            String username = safeUsername(request);
            logStructured(context, "auth_login_request", "failed", reason, correlationId,
                    elapsedMillis(startNanos), safeCpf(request), username,
                    "Tentativa de login negada para usuario " + username, Map.of("errorMessage", ex.getMessage()));
            authTelemetry.recordLoginFailure(
                    context,
                    safeCpf(request),
                    correlationId,
                    reason,
                    elapsedMillis(startNanos)
            );
            return ApiResponseFactory.error(401, ex.getMessage());
        } catch (Exception ex) {
            log(context, "Erro interno no login");
            String username = safeUsername(request);
            logStructured(context, "auth_login_request", "failed", "internal_error", correlationId,
                    elapsedMillis(startNanos), safeCpf(request), username,
                    "Erro interno durante autenticacao do usuario " + username,
                    Map.of("errorType", ex.getClass().getSimpleName()));
            authTelemetry.recordLoginFailure(context, safeCpf(request), correlationId, "internal_error", elapsedMillis(startNanos));
            return ApiResponseFactory.error(500, "Erro interno");
        }
    }

    private long elapsedMillis(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }

    private String resolveCorrelationId(Map<String, Object> event, Context context) {
        if (event != null) {
            Object headersValue = event.get("headers");
            if (headersValue instanceof Map<?, ?> headers) {
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

    private String safeCpf(LoginRequest request) {
        return request == null ? null : extractCpf(request.username());
    }

    private String safeUsername(LoginRequest request) {
        if (request == null || request.username() == null || request.username().isBlank()) {
            return "unknown";
        }
        return request.username();
    }

    private String extractCpf(String username) {
        if (username == null) {
            return null;
        }
        String digits = username.replaceAll("\\D", "");
        return digits.length() == 11 ? digits : null;
    }

    private String classifyUnauthorizedReason(String message) {
        if (message == null) {
            return "unauthorized";
        }
        String normalized = message.toLowerCase();
        if (normalized.contains("desabilitado")) {
            return "user_disabled";
        }
        if (normalized.contains("credenciais")) {
            return "invalid_credentials";
        }
        return "unauthorized";
    }

    private LoginRequest parseRequest(Map<String, Object> event) {
        Object body = event == null ? null : event.get("body");
        boolean base64Encoded = event != null && Boolean.TRUE.equals(event.get("isBase64Encoded"));
        String json = JsonSupport.decodeBody(body, base64Encoded);
        if (json == null || json.isBlank()) {
            throw new IllegalArgumentException("Corpo da requisicao e obrigatorio");
        }
        return JsonSupport.readValue(json, LoginRequest.class);
    }

    private void log(Context context, String message) {
        if (context != null && context.getLogger() != null) {
            context.getLogger().log(message + "\n");
        }
    }

    private void logStructured(
            Context context,
            String eventName,
            String stage,
            String reason,
            String correlationId,
            long latencyMs,
            String cpf,
            String username,
            String message,
            Map<String, Object> extras) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("event", eventName);
        payload.put("flow", "login");
        payload.put("stage", stage);
        payload.put("reason", reason);
        payload.put("message", message);
        payload.put("correlationId", correlationId);
        payload.put("latencyMs", latencyMs);
        payload.put("cpf", cpf);
        payload.put("username", username);
        payload.put("timestamp", Instant.now().toString());

        if (context != null) {
            payload.put("awsRequestId", context.getAwsRequestId());
            payload.put("functionName", context.getFunctionName());
        }

        if (extras != null && !extras.isEmpty()) {
            payload.putAll(extras);
        }

        String level = resolveLevel(stage);
        StructuredLogger.emit(context, level, payload);
    }

    private String resolveLevel(String stage) {
        if (stage == null) return "INFO";
        return switch (stage) {
            case "failed" -> "WARN";
            default -> "INFO";
        };
    }
}

