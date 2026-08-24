package br.com.fiap.infrastructure.configuration;

import br.com.fiap.domain.ports.in.AuthenticationInputPort;
import br.com.fiap.domain.ports.out.PasswordVerifierPort;
import br.com.fiap.domain.ports.out.TokenServicePort;
import br.com.fiap.domain.ports.out.UserRepositoryPort;
import br.com.fiap.domain.usecases.AuthenticationUseCase;
import br.com.fiap.infrastructure.adapters.crypto.BCryptPasswordAdapter;
import br.com.fiap.infrastructure.adapters.jwt.JwtTokenAdapter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Realiza o wiring entre o domínio e os adapters de infraestrutura.
 */
@Configuration
public class AuthBeanConfiguration {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public PasswordVerifierPort passwordVerifierPort(PasswordEncoder passwordEncoder) {
        return new BCryptPasswordAdapter(passwordEncoder);
    }

    @Bean
    public TokenServicePort tokenServicePort(JwtTokenAdapter jwtTokenAdapter) {
        return jwtTokenAdapter;
    }

    @Bean
    public AuthenticationInputPort authenticationInputPort(UserRepositoryPort userRepositoryPort,
                                                           TokenServicePort tokenServicePort,
                                                           PasswordVerifierPort passwordVerifierPort) {
        return new AuthenticationUseCase(userRepositoryPort, tokenServicePort, passwordVerifierPort);
    }
}
