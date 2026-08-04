# RFC-001 — Estratégia de Autenticação Serverless com AWS Lambda e JWT

| Campo        | Valor                                              |
|--------------|----------------------------------------------------|
| **RFC**      | 001                                                |
| **Título**   | Autenticação Serverless via AWS Lambda + JWT HS256 |
| **Repositório** | fiap-14soat-tc-fase5-auth-lambda                   |
| **Status**   | Aceito                                             |
| **Autor**    | Time FIAP 14SOAT Fase 5                           |
| **Data**     | 2026-04-20                                         |

---

## 1. Resumo

Este documento descreve e justifica a decisão de implementar a autenticação da plataforma RaceForce API utilizando **AWS Lambda** (runtime Java 21) integrada ao **AWS API Gateway HTTP API v2**, emitindo tokens **JWT assinados com HS256**.

---

## 2. Motivação

O enunciado do Tech Challenge Fase 5 exige:

- Autenticação via **CPF** do cliente;
- Uso de **Function Serverless** para validar identidade e emitir tokens;
- Proteção de rotas sensíveis via **API Gateway**.

A equipe avaliou três estratégias:

| Opção | Descrição | Descartado por |
|-------|-----------|----------------|
| A | Spring Security integrado ao pod EKS | Acoplamento forte; não atende ao requisito serverless |
| B | AWS Cognito User Pool | Custo adicional; complexidade de configuração no AWS Academy; CPF não é campo nativo |
| **C (escolhida)** | **Lambda Java 21 + API Gateway + JWT** | Atende todos os requisitos; custo zero no Academy; controle total sobre o fluxo |

---

## 3. Proposta

### 3.1 Componentes

```
Cliente → POST /auth/login → API Gateway → Lambda Login (LoginHandler)
                                            ↓
                                    SELECT users/service_order (RDS PostgreSQL)
                                            ↓
                                    Gera JWT (HS256, expira em 24h)
                                            ↓
                              Retorna: { token, role, expiresIn, username }

Cliente → GET /service-order → API Gateway → Lambda Authorizer (JwtAuthorizerHandler)
                                              ↓
                                    Valida assinatura JWT + claims
                                              ↓
                                    Encaminha para EKS (se válido) ou 401/403
```

### 3.2 Regras de Validação de Identidade

1. Busca usuário primeiro na tabela `users` (admin e usuários cadastrados);
2. Se não encontrado, busca na tabela `service_order` pelo CPF do cliente;
3. Senha padrão para clientes = **primeiros 3 dígitos do CPF**;
4. Retorna role `ADMIN` para usuários da tabela `users` com flag admin, ou `USER` para demais.

### 3.3 Segredo JWT

- Variável de ambiente `JWT_SECRET` sincronizada entre Lambda e aplicação Spring Boot;
- Issuer: `RaceforceApi`;
- Expiração: `86400000 ms` (24 horas).

### 3.4 Rotas Protegidas pelo Authorizer

| Grupo | Rotas                                                                                                          | Autenticação |
|-------|----------------------------------------------------------------------------------------------------------------|-------------|
| Públicas | `POST /auth/login`, `GET /healthz`                                                                             | Nenhuma |
| Protegidas | `/customer`, `/vehicle`, `/service`, `/product`, `GET /service-order` (Conforme requisitos do Tech Challenge 1 | JWT Bearer obrigatório |
| Catch-all | `ANY /{proxy+}`                                                                                                | Nenhuma (fallback) |

---

## 4. Alternativas Rejeitadas

### 4.1 AWS Cognito
- Não disponível de forma gratuita no AWS Academy;
- CPF requereria atributo customizado e lógica adicional;
- Overhead operacional desnecessário para o escopo acadêmico.

### 4.2 Basic Auth no Spring Security
- Não atende ao requisito de serverless;
- Manteria o pod como ponto único de autenticação (sem separação de responsabilidades).

### 4.3 OAuth2 / OIDC externo (Auth0, Okta)
- Dependência de serviço externo pago;
- Fora do escopo de controle da infraestrutura AWS Academy.

---

## 5. Consequências

### Positivas
- Separação clara entre autenticação (Lambda) e negócio (EKS);
- Custo zero de execução no AWS Academy (Lambda free tier);
- Escalabilidade automática da função de autenticação;
- Facilidade de evolução: trocar estratégia de validação sem afetar a aplicação.

### Negativas / Riscos
- **Cold start Java 21**: primeiras invocações podem ter latência de 2-5s. Mitigação: SnapStart ou migração para GraalVM Native em versões futuras;
- `ANY /{proxy+}` (catch-all) não tem autenticação — rotas como `POST /service-order` não estão protegidas nesta fase. Ação futura: adicionar `ANY /service-order` às rotas protegidas.

---

## 6. Referências

- [AWS API Gateway HTTP API — Lambda Authorizer](https://docs.aws.amazon.com/apigateway/latest/developerguide/http-api-lambda-authorizer.html)
- [RFC 7519 — JSON Web Token (JWT)](https://datatracker.ietf.org/doc/html/rfc7519)
- [RFC 7518 — JSON Web Algorithms (JWA)](https://datatracker.ietf.org/doc/html/rfc7518)

