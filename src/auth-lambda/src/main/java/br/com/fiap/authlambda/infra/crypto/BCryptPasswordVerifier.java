package br.com.fiap.authlambda.infra.crypto;

import br.com.fiap.authlambda.domain.port.PasswordVerifier;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class BCryptPasswordVerifier implements PasswordVerifier {

    private final BCryptPasswordEncoder encoder;

    public BCryptPasswordVerifier() {
        this(new BCryptPasswordEncoder());
    }

    public BCryptPasswordVerifier(BCryptPasswordEncoder encoder) {
        this.encoder = encoder;
    }

    @Override
    public boolean matches(String rawPassword, String encodedPassword) {
        return encoder.matches(rawPassword, encodedPassword);
    }
}

