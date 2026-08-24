package br.com.fiap.application;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Inicializa o microserviço de autenticação baseado em Spring Boot.
 */
@SpringBootApplication(scanBasePackages = "br.com.fiap")
@EntityScan(basePackages = "br.com.fiap.infrastructure.entities")
@EnableJpaRepositories(basePackages = "br.com.fiap.infrastructure.adapters.repositories")
public class AuthApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthApplication.class, args);
    }
}
