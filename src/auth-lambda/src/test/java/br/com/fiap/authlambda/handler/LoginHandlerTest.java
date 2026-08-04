package br.com.fiap.authlambda.handler;

import br.com.fiap.authlambda.domain.model.LoginResponse;
import br.com.fiap.authlambda.domain.port.AuthenticationUseCase;
import br.com.fiap.authlambda.domain.service.UnauthorizedException;
import br.com.fiap.authlambda.support.observability.AuthTelemetry;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LoginHandlerTest {

    @Test
    void shouldReturnTokenForValidLogin() {
        AuthenticationUseCase service = Mockito.mock(AuthenticationUseCase.class);
        AuthTelemetry telemetry = Mockito.mock(AuthTelemetry.class);
        when(service.authenticate(any())).thenReturn(new LoginResponse("jwt-token", "Bearer", 123L, "admin", "ADMIN", "AWS Lambda"));

        LoginHandler handler = new LoginHandler(service, telemetry);
        Map<String, Object> response = handler.handleRequest(Map.of("body", "{\"username\":\"admin\",\"password\":\"admin123\"}"), null);

        assertEquals(200, response.get("statusCode"));
        assertTrue(response.get("body").toString().contains("jwt-token"));
        assertTrue(response.get("body").toString().contains("AWS Lambda"));
        verify(telemetry).recordLoginSuccess(any(), eq(null), any(), anyLong());
        verify(telemetry).recordTokenIssued(any(), eq(null), any());
    }

    @Test
    void shouldReturn401WhenAuthenticationFails() {
        AuthenticationUseCase service = Mockito.mock(AuthenticationUseCase.class);
        AuthTelemetry telemetry = Mockito.mock(AuthTelemetry.class);
        when(service.authenticate(any())).thenThrow(new UnauthorizedException("Credenciais invalidas"));

        LoginHandler handler = new LoginHandler(service, telemetry);
        Map<String, Object> response = handler.handleRequest(Map.of("body", "{\"username\":\"admin\",\"password\":\"wrong\"}"), null);

        assertEquals(401, response.get("statusCode"));
        assertTrue(response.get("body").toString().contains("Credenciais invalidas"));
        verify(telemetry).recordLoginFailure(any(), eq(null), any(), eq("invalid_credentials"), anyLong());
    }
}

