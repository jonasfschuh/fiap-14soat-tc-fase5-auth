package br.com.fiap.authlambda.domain.port;

public interface PasswordVerifier {
    boolean matches(String rawPassword, String encodedPassword);
}

