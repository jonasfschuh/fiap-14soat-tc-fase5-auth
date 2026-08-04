package br.com.fiap.authlambda.domain.port;

import br.com.fiap.authlambda.domain.model.AuthenticatedPrincipal;
import br.com.fiap.authlambda.domain.model.UserAccount;

public interface TokenService {
    String generateToken(UserAccount user);

    AuthenticatedPrincipal validateToken(String token);
}

