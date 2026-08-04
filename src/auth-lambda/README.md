# Auth Lambda

Projeto Java 21 para AWS Lambda com login JWT e Lambda Authorizer.

## Estrutura

```text
src/auth-lambda/
  pom.xml
  template.yaml
  README.md
  src/main/java/br/com/fiap/authlambda/
  src/test/java/br/com/fiap/authlambda/
```

## Requisitos

- Java 21
- Maven 3.9+
- AWS SAM CLI
- PostgreSQL acessível pela Lambda

## Variáveis de ambiente / parâmetros

- `JWT_SECRET`
- `JWT_ISSUER`
- `JWT_EXPIRATION_MS`
- `DB_URL`
- `DB_USER`
- `DB_PASSWORD`
- `DB_POOL_SIZE`

## Build e testes

```powershell
mvn clean test
mvn clean package
```

## Validação do template SAM

```powershell
sam validate --template-file template.yaml
sam build --template-file template.yaml
```

## Deploy

Exemplo com parâmetros explícitos:

```powershell
sam deploy --guided --template-file template.yaml
```

## Observação de segurança

Os segredos não devem ser hardcoded. Em ambiente real, prefira injetar `JWT_SECRET` e credenciais do banco via AWS Secrets Manager ou SSM Parameter Store.

