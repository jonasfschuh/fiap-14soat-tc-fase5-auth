package br.com.fiap.infrastructure.adapters.repositories;

import br.com.fiap.infrastructure.entities.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Define o repositório JPA para acesso à tabela de usuários.
 */
public interface UserJpaRepository extends JpaRepository<UserEntity, Long> {

    /**
     * Busca uma entidade de usuário pelo username.
     *
     * @param username username persistido
     * @return entidade encontrada, quando existir
     */
    Optional<UserEntity> findByUsername(String username);
}
