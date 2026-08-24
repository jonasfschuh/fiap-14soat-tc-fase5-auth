package br.com.fiap.domain.usecases;

import br.com.fiap.domain.exceptions.UnauthorizedException;
import br.com.fiap.domain.model.LoginRequest;
import br.com.fiap.domain.model.Role;
import br.com.fiap.domain.model.UserAccount;
import br.com.fiap.domain.ports.out.PasswordVerifierPort;
import br.com.fiap.domain.ports.out.TokenServicePort;
import br.com.fiap.domain.ports.out.UserRepositoryPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Valida o comportamento principal do caso de uso de autenticação.
 */
@ExtendWith(MockitoExtension.class)
class AuthenticationUseCaseTest {

    @Mock
    private UserRepositoryPort userRepositoryPort;

    @Mock
    private TokenServicePort tokenServicePort;

    @Mock
    private PasswordVerifierPort passwordVerifierPort;

    @InjectMocks
    private AuthenticationUseCase authenticationUseCase;

    @Test
    @DisplayName("Deve autenticar usuário ativo com credenciais válidas")
    void shouldAuthenticateActiveUser() {
        UserAccount user = new UserAccount(1L, "admin", "hash", Role.ADMIN, true);
        when(userRepositoryPort.findByUsername("admin")).thenReturn(Optional.of(user));
        when(passwordVerifierPort.matches("admin123", "hash")).thenReturn(true);
        when(tokenServicePort.generate(user)).thenReturn("jwt-token");

        var response = authenticationUseCase.login(new LoginRequest("admin", "admin123"));

        assertThat(response.token()).isEqualTo("jwt-token");
        assertThat(response.username()).isEqualTo("admin");
        assertThat(response.role()).isEqualTo("ADMIN");
    }

    @Test
    @DisplayName("Deve rejeitar usuário inexistente")
    void shouldRejectUnknownUser() {
        when(userRepositoryPort.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authenticationUseCase.login(new LoginRequest("ghost", "123")))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Credenciais inválidas.");
    }
}
