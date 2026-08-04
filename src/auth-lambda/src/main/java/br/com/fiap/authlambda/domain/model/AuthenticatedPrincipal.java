package br.com.fiap.authlambda.domain.model;

public record AuthenticatedPrincipal(String username, String role, String email) {
}

