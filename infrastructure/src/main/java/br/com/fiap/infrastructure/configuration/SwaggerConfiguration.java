package br.com.fiap.infrastructure.configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * Configura a documentação OpenAPI seguindo o padrão dos demais serviços.
 */
@Configuration
public class SwaggerConfiguration {

    /**
     * URL do server exibida no Swagger UI (ex.: /auth quando acessado via Ingress).
     * Padrão "/" (relativo) para uso local direto na porta do serviço (8090).
     */
    @Value("${swagger.server.url:/}")
    private String swaggerServerUrl;

    @Bean
    public OpenAPI customOpenAPI() {
        String serverUrl = StringUtils.hasText(swaggerServerUrl) ? swaggerServerUrl : "/";
        return new OpenAPI()
                .servers(List.of(new Server().url(serverUrl).description("Ambiente local ou cluster")))
                .components(new Components()
                        .addSecuritySchemes("bearer-jwt", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("JWT obtido via POST /auth/login. Informe: Bearer <token>")))
                .addSecurityItem(new SecurityRequirement().addList("bearer-jwt"))
                .info(new Info()
                        .title("FIAP X - Auth Service")
                        .version("1.0.0")
                        .description("Microserviço de autenticação responsável por login e validação de tokens JWT."));
    }
}
