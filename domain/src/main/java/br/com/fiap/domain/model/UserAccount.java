package br.com.fiap.domain.model;

/**
 * Representa a conta de usuário utilizada no processo de autenticação.
 */
public record UserAccount(Long id, String username, String passwordHash, Role role, boolean active) {
}
