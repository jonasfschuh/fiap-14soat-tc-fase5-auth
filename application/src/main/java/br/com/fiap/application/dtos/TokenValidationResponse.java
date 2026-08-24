package br.com.fiap.application.dtos;

/**
 * Representa o resultado da validação de um token autenticado.
 */
public record TokenValidationResponse(boolean valid, String username, String role) {
}
