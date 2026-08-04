package br.com.fiap.authlambda.config;

public record JwtSettings(String secret, String issuer, long expirationMs) {
}

