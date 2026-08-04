package br.com.fiap.authlambda.domain.model;

public record LoginRequest(String username, String password) {

    public LoginRequest {
        username = username == null ? null : username.trim();
    }

    public void validate() {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("username e obrigatorio");
        }
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("password e obrigatorio");
        }
    }
}

