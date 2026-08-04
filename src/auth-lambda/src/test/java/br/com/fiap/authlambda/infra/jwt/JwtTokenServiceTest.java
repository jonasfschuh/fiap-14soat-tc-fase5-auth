package br.com.fiap.authlambda.infra.jwt;

import br.com.fiap.authlambda.config.JwtSettings;
import br.com.fiap.authlambda.domain.model.AuthenticatedPrincipal;
import br.com.fiap.authlambda.domain.model.Role;
import br.com.fiap.authlambda.domain.model.UserAccount;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JwtTokenServiceTest {

    private final Clock clock = Clock.systemUTC();
    private final JwtSettings jwtSettings = new JwtSettings("test-secret-for-unit-tests-only", "auth-lambda", 86_400_000L);

    @Test
    void shouldGenerateAndValidateToken() {
        JwtTokenService service = new JwtTokenService(jwtSettings, clock);
        UserAccount user = new UserAccount(
                1L,
                "admin",
                "hash",
                "admin@fiap.com.br",
                Role.ADMIN,
                true,
                Instant.now(),
                Instant.now(),
                null
        );

        String token = service.generateToken(user);
        AuthenticatedPrincipal principal = service.validateToken(token);

        assertEquals("admin", principal.username());
        assertEquals("ADMIN", principal.role());
        assertEquals("admin@fiap.com.br", principal.email());
    }
}

