package br.com.fiap.domain.model;

/**
 * Representa a resposta de autenticação devolvida ao cliente.
 */
public record LoginResponse(String token, String username, String role) {
}
