package br.com.fiap.infrastructure.adapters.crypto;

import br.com.fiap.domain.ports.out.PasswordVerifierPort;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Objects;

/**
 * Implementa a verificação de senhas usando BCrypt do Spring Security.
 */
public class BCryptPasswordAdapter implements PasswordVerifierPort {

    private final PasswordEncoder passwordEncoder;

    public BCryptPasswordAdapter(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = Objects.requireNonNull(passwordEncoder, "passwordEncoder");
    }

    @Override
    public boolean matches(String rawPassword, String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }
}
