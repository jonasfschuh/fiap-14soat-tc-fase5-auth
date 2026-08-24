package br.com.fiap.domain.model;

/**
 * Representa os dados de entrada necessários para autenticar um usuário.
 */
public record LoginRequest(String username, String password) {

    public LoginRequest {
        username = username == null ? null : username.trim();
    }

    /**
     * Valida os dados obrigatórios do login.
     */
    public void validate() {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username é obrigatório.");
        }
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("Password é obrigatório.");
        }
    }
}
