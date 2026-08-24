package br.com.fiap.application.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * Representa o payload HTTP de login recebido pela API.
 */
public record LoginRequest(
        @NotBlank @Schema(example = "admin") String username,
        @NotBlank @Schema(example = "admin123") String password) {
}
