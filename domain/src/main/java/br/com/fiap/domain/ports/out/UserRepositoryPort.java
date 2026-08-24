package br.com.fiap.domain.ports.out;

import br.com.fiap.domain.model.UserAccount;

import java.util.Optional;

/**
 * Define a porta de saída para consulta de usuários persistidos.
 */
public interface UserRepositoryPort {

    /**
     * Busca um usuário pelo username.
     *
     * @param username identificador do usuário
     * @return usuário encontrado, quando existir
     */
    Optional<UserAccount> findByUsername(String username);
}
