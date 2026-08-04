package br.com.fiap.authlambda.domain.port;

import br.com.fiap.authlambda.domain.model.UserAccount;

import java.time.Instant;
import java.util.Optional;

public interface UserRepository {
    Optional<UserAccount> findByUsername(String username);

    default Optional<UserAccount> findByCustomerCpfInServiceOrder(String cpf) {
        return Optional.empty();
    }

    void updateLastLogin(long userId, Instant lastLogin);
}

