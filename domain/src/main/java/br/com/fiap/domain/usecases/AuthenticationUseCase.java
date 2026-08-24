package br.com.fiap.domain.usecases;

import br.com.fiap.domain.exceptions.UnauthorizedException;
import br.com.fiap.domain.model.LoginRequest;
import br.com.fiap.domain.model.LoginResponse;
import br.com.fiap.domain.model.UserAccount;
import br.com.fiap.domain.ports.in.AuthenticationInputPort;
import br.com.fiap.domain.ports.out.PasswordVerifierPort;
import br.com.fiap.domain.ports.out.TokenServicePort;
import br.com.fiap.domain.ports.out.UserRepositoryPort;

import java.util.Objects;

/**
 * Implementa o caso de uso responsável por autenticar usuários e emitir tokens.
 */
public class AuthenticationUseCase implements AuthenticationInputPort {

    private final UserRepositoryPort userRepositoryPort;
    private final TokenServicePort tokenServicePort;
    private final PasswordVerifierPort passwordVerifierPort;

    public AuthenticationUseCase(UserRepositoryPort userRepositoryPort,
                                 TokenServicePort tokenServicePort,
                                 PasswordVerifierPort passwordVerifierPort) {
        this.userRepositoryPort = Objects.requireNonNull(userRepositoryPort, "userRepositoryPort");
        this.tokenServicePort = Objects.requireNonNull(tokenServicePort, "tokenServicePort");
        this.passwordVerifierPort = Objects.requireNonNull(passwordVerifierPort, "passwordVerifierPort");
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        Objects.requireNonNull(request, "request");
        request.validate();

        UserAccount user = userRepositoryPort.findByUsername(request.username())
                .filter(UserAccount::active)
                .orElseThrow(() -> new UnauthorizedException("Credenciais inválidas."));

        if (!passwordVerifierPort.matches(request.password(), user.passwordHash())) {
            throw new UnauthorizedException("Credenciais inválidas.");
        }

        String token = tokenServicePort.generate(user);
        return new LoginResponse(token, user.username(), user.role().name());
    }
}
