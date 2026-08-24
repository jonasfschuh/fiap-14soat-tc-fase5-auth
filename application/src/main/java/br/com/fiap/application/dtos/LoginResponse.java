package br.com.fiap.application.dtos;

/**
 * Representa o payload HTTP devolvido após um login bem-sucedido.
 */
public record LoginResponse(String token, String username, String role) {
}
