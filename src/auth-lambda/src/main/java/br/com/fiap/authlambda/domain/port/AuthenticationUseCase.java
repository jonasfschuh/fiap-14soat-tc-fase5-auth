package br.com.fiap.authlambda.domain.port;

import br.com.fiap.authlambda.domain.model.LoginRequest;
import br.com.fiap.authlambda.domain.model.LoginResponse;

public interface AuthenticationUseCase {
    LoginResponse authenticate(LoginRequest request);
}

