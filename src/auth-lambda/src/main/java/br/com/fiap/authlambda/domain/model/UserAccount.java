package br.com.fiap.authlambda.domain.model;

import java.time.Instant;

public record UserAccount(
        long id,
        String username,
        String passwordHash,
        String email,
        Role role,
        boolean enabled,
        Instant createdAt,
        Instant updatedAt,
        Instant lastLogin
) {
}

