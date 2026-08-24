package br.com.fiap.infrastructure.configuration;

import br.com.fiap.infrastructure.adapters.jwt.JwtTokenAdapter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Centraliza a criação dos componentes responsáveis pelo JWT.
 */
@Configuration
public class JwtConfiguration {

    @Bean
    public JwtTokenAdapter jwtTokenAdapter(@Value("${auth.jwt.secret}") String secret,
                                           @Value("${auth.jwt.expiration-ms}") long expirationMs,
                                           @Value("${auth.jwt.issuer}") String issuer) {
        return new JwtTokenAdapter(secret, expirationMs, issuer);
    }
}
