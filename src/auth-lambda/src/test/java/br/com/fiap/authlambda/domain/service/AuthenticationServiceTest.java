package br.com.fiap.authlambda.domain.service;

import br.com.fiap.authlambda.config.JwtSettings;
import br.com.fiap.authlambda.domain.model.LoginRequest;
import br.com.fiap.authlambda.domain.model.LoginResponse;
import br.com.fiap.authlambda.domain.model.Role;
import br.com.fiap.authlambda.domain.model.UserAccount;
import br.com.fiap.authlambda.domain.port.PasswordVerifier;
import br.com.fiap.authlambda.domain.port.TokenService;
import br.com.fiap.authlambda.domain.port.UserRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthenticationServiceTest {

    private final Clock clock = Clock.fixed(Instant.parse("2024-01-01T10:15:30Z"), ZoneOffset.UTC);
    private final JwtSettings jwtSettings = new JwtSettings("test-secret-for-unit-tests-only", "auth-lambda", 86_400_000L);

    @Test
    void shouldAuthenticateValidUser() {
        UserRepository userRepository = Mockito.mock(UserRepository.class);
        PasswordVerifier passwordVerifier = Mockito.mock(PasswordVerifier.class);
        TokenService tokenService = Mockito.mock(TokenService.class);

        UserAccount user = new UserAccount(
                1L,
                "admin",
                "$2a$10$hash",
                "admin@fiap.com.br",
                Role.ADMIN,
                true,
                Instant.parse("2024-01-01T00:00:00Z"),
                Instant.parse("2024-01-01T00:00:00Z"),
                null
        );

        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));
        when(passwordVerifier.matches("admin123", "$2a$10$hash")).thenReturn(true);
        when(tokenService.generateToken(user)).thenReturn("jwt-token");

        AuthenticationService service = new AuthenticationService(userRepository, passwordVerifier, tokenService, clock, jwtSettings);
        LoginResponse response = service.authenticate(new LoginRequest("admin", "admin123"));

        assertEquals("jwt-token", response.token());
        assertEquals("Bearer", response.tokenType());
        assertEquals(86_400_000L, response.expiresIn());
        assertEquals("admin", response.username());
        assertEquals("ADMIN", response.role());
        verify(userRepository).updateLastLogin(eq(1L), any(Instant.class));
    }

    @Test
    void shouldRejectInvalidPassword() {
        UserRepository userRepository = Mockito.mock(UserRepository.class);
        PasswordVerifier passwordVerifier = Mockito.mock(PasswordVerifier.class);
        TokenService tokenService = Mockito.mock(TokenService.class);

        UserAccount user = new UserAccount(
                1L,
                "user",
                "$2a$10$hash",
                "user@fiap.com.br",
                Role.USER,
                true,
                Instant.parse("2024-01-01T00:00:00Z"),
                Instant.parse("2024-01-01T00:00:00Z"),
                null
        );

        when(userRepository.findByUsername("user")).thenReturn(Optional.of(user));
        when(passwordVerifier.matches("wrong", "$2a$10$hash")).thenReturn(false);

        AuthenticationService service = new AuthenticationService(userRepository, passwordVerifier, tokenService, clock, jwtSettings);

        assertThrows(UnauthorizedException.class, () -> service.authenticate(new LoginRequest("user", "wrong")));
        verify(userRepository, never()).updateLastLogin(eq(1L), any(Instant.class));
    }

    @Test
    void shouldAuthenticateAdminFallbackWhenDbUnavailable() {
        UserRepository userRepository = Mockito.mock(UserRepository.class);
        PasswordVerifier passwordVerifier = Mockito.mock(PasswordVerifier.class);
        TokenService tokenService = Mockito.mock(TokenService.class);

        when(userRepository.findByUsername("admin"))
                .thenThrow(new IllegalStateException("Erro ao consultar usuario"));
        when(tokenService.generateToken(any())).thenReturn("jwt-admin-fallback-token");

        AuthenticationService service = new AuthenticationService(userRepository, passwordVerifier, tokenService, clock, jwtSettings);
        LoginResponse response = service.authenticate(new LoginRequest("admin", "admin123"));

        assertEquals("jwt-admin-fallback-token", response.token());
        assertEquals("admin", response.username());
        assertEquals("ADMIN", response.role());
        verify(userRepository, never()).updateLastLogin(any(Long.class), any(Instant.class));
    }

    @Test
    void shouldRejectWrongCredentialsWhenDbUnavailable() {
        UserRepository userRepository = Mockito.mock(UserRepository.class);
        PasswordVerifier passwordVerifier = Mockito.mock(PasswordVerifier.class);
        TokenService tokenService = Mockito.mock(TokenService.class);

        when(userRepository.findByUsername("hacker"))
                .thenThrow(new IllegalStateException("Erro ao consultar usuario"));

        AuthenticationService service = new AuthenticationService(userRepository, passwordVerifier, tokenService, clock, jwtSettings);
        assertThrows(UnauthorizedException.class, () -> service.authenticate(new LoginRequest("hacker", "qualquersenha")));
    }

    @Test
    void shouldAuthenticateUsingServiceOrderCpfFallback() {
        UserRepository userRepository = Mockito.mock(UserRepository.class);
        PasswordVerifier passwordVerifier = Mockito.mock(PasswordVerifier.class);
        TokenService tokenService = Mockito.mock(TokenService.class);
        PrintStream originalOut = System.out;
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        System.setOut(new PrintStream(output));

        UserAccount fallbackUser = new UserAccount(
                -1L,
                "02772020940",
                "027",
                "02772020940@local",
                Role.USER,
                true,
                null,
                null,
                null
        );

        when(userRepository.findByUsername("02772020940")).thenReturn(Optional.empty());
        when(userRepository.findByCustomerCpfInServiceOrder("02772020940")).thenReturn(Optional.of(fallbackUser));
        when(tokenService.generateToken(fallbackUser)).thenReturn("jwt-fallback-token");

        try {
            AuthenticationService service = new AuthenticationService(userRepository, passwordVerifier, tokenService, clock, jwtSettings);
            LoginResponse response = service.authenticate(new LoginRequest("02772020940", "027"));

            assertEquals("jwt-fallback-token", response.token());
            assertEquals("02772020940", response.username());
            assertEquals("USER", response.role());
            assertEquals(true, output.toString().contains("Logado e autenticado com CPF 02772020940"));
            assertEquals(true, output.toString().contains("authMethod=cpf_fallback"));
            verify(userRepository, never()).updateLastLogin(any(Long.class), any(Instant.class));
            verify(passwordVerifier, never()).matches(any(), any());
        } finally {
            System.setOut(originalOut);
        }
    }
}

