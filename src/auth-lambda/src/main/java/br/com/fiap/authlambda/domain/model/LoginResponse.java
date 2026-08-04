package br.com.fiap.authlambda.domain.model;

public record LoginResponse(
        String token,
        String tokenType,
        long expiresIn,
        String username,
        String role,
        String authProvider
) {
}

