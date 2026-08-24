package br.com.fiap.infrastructure.adapters.repositories;

import br.com.fiap.domain.model.UserAccount;
import br.com.fiap.domain.ports.out.UserRepositoryPort;
import br.com.fiap.infrastructure.entities.UserEntity;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Implementa a porta de usuários do domínio a partir do repositório JPA.
 */
@Repository
public class UserRepositoryImpl implements UserRepositoryPort {

    private final UserJpaRepository userJpaRepository;

    public UserRepositoryImpl(UserJpaRepository userJpaRepository) {
        this.userJpaRepository = userJpaRepository;
    }

    @Override
    public Optional<UserAccount> findByUsername(String username) {
        return userJpaRepository.findByUsername(username).map(this::toDomain);
    }

    private UserAccount toDomain(UserEntity entity) {
        return new UserAccount(entity.getId(), entity.getUsername(), entity.getPasswordHash(), entity.getRole(), entity.isActive());
    }
}
