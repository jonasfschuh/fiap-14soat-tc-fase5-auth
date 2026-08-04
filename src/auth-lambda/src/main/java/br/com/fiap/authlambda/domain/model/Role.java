package br.com.fiap.authlambda.domain.model;

import java.util.Locale;

public enum Role {
    USER,
    ADMIN;

    public static Role fromValue(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Role nao pode ser vazia");
        }
        return Role.valueOf(value.trim().toUpperCase(Locale.ROOT));
    }
}

