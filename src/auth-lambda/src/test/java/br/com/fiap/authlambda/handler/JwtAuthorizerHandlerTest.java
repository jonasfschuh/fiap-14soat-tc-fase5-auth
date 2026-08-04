package br.com.fiap.authlambda.handler;

import br.com.fiap.authlambda.config.JwtSettings;
import br.com.fiap.authlambda.domain.model.AuthenticatedPrincipal;
import br.com.fiap.authlambda.domain.model.Role;
import br.com.fiap.authlambda.domain.model.UserAccount;
import br.com.fiap.authlambda.domain.port.TokenService;
import br.com.fiap.authlambda.infra.jwt.JwtTokenService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class JwtAuthorizerHandlerTest {

    private final Clock clock = Clock.systemUTC();
    private final JwtSettings jwtSettings = new JwtSettings("test-secret-for-unit-tests-only", "auth-lambda", 86_400_000L);

    @Test
    void shouldAuthorizeValidToken() {
        JwtTokenService tokenService = new JwtTokenService(jwtSettings, clock);
        String token = tokenService.generateToken(new UserAccount(
                1L,
                "admin",
                "hash",
                "admin@fiap.com.br",
                Role.ADMIN,
                true,
                Instant.now(),
                Instant.now(),
                null
        ));

        JwtAuthorizerHandler handler = new JwtAuthorizerHandler(tokenService);
        Map<String, Object> response = handler.handleRequest(Map.of(
                "headers", Map.of("Authorization", "Bearer " + token)
        ), null);

        assertTrue(Boolean.TRUE.equals(response.get("isAuthorized")));
        Map<String, Object> context = (Map<String, Object>) response.get("context");
        assertEquals("admin", context.get("sub"));
        assertEquals("admin", context.get("username"));
        assertEquals("ADMIN", context.get("role"));
    }

    @Test
    void shouldDenyInvalidToken() {
        TokenService tokenService = Mockito.mock(TokenService.class);
        when(tokenService.validateToken(anyString())).thenThrow(new RuntimeException("invalid token"));
        JwtAuthorizerHandler handler = new JwtAuthorizerHandler(tokenService);

        Map<String, Object> response = handler.handleRequest(Map.of(
                "headers", Map.of("Authorization", "Bearer invalid.token.here")
        ), null);

        assertFalse(Boolean.TRUE.equals(response.get("isAuthorized")));
    }
}

