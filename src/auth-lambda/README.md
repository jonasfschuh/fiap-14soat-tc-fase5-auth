# Auth Service

Módulo Java 21 com Spring Boot 3 para autenticação JWT da plataforma RaceForce.

## Estrutura

```text
src/auth-lambda/
  pom.xml
  template.yaml   (legado — mantido para referência histórica)
  README.md
  src/main/java/br/com/fiap/authlambda/
  src/test/java/br/com/fiap/authlambda/
```

## Requisitos

- Java 21
- Maven 3.9+
- PostgreSQL acessível pela aplicação

## Variáveis de ambiente

- `JWT_SECRET`
- `JWT_ISSUER`
- `JWT_EXPIRATION_MS`
- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `SERVER_PORT`

## Build e testes

```bash
mvn clean test
mvn clean package
```

## Observação de segurança

Os segredos não devem ser hardcoded. Injete `JWT_SECRET` e credenciais do banco via
variáveis de ambiente, secrets do Kubernetes ou um gerenciador de secrets (ex.: Vault, Sealed Secrets).