# RFC-001 — Estratégia de Autenticação via Serviço Spring Boot + JWT HS256 (100% local)

| Campo        | Valor                                                                |
|--------------|------------------------------------------------------------------------|
| **RFC**      | 001                                                                  |
| **Título**   | Autenticação centralizada no serviço `auth` (Spring Boot + JWT HS256), sem dependência de nuvem |
| **Repositório** | fiap-14soat-tc-fase5-auth                                            |
| **Status**   | Aceito — **revisado em 2026-09-10** (substitui a versão anterior baseada em AWS Lambda) |
| **Autor**    | Time FIAP 14SOAT Fase 5                                              |
| **Data**     | 2026-04-20 (criado) · 2026-09-10 (revisado para a realidade 100% local) |

---

## ⚠️ Nota sobre a revisão

A versão original deste RFC descrevia autenticação **serverless via AWS Lambda + API Gateway**, com login por CPF e consulta a uma tabela `service_order` — este fluxo pertence a um domínio de negócio diferente (sistema de oficina mecânica de uma fase anterior do curso) e **não corresponde ao projeto atual** (plataforma de processamento de vídeos). Esta revisão substitui o conteúdo pela arquitetura real: um serviço Spring Boot dedicado (`auth`), sem AWS, autenticando por `username`/`password` contra a tabela `users`.

---

## 1. Resumo

Este documento descreve e justifica a decisão de implementar a autenticação da plataforma de processamento de vídeos por meio de um **microsserviço dedicado `auth`** (Spring Boot 3 / Java 21), que emite e valida tokens **JWT assinados com HS256**, rodando como `Deployment` em um **cluster Kubernetes local (Docker Desktop)** — sem nenhuma dependência de serviços gerenciados de nuvem (Lambda, API Gateway, Cognito).

---

## 2. Motivação

O enunciado do Hackathon (`docs/requirements/POSTECH - SOAT - Fase 5 - Hacka.txt`) exige, nos Requisitos Funcionais:

> *"O Sistema deve ser protegido por usuário e senha."*

A equipe avaliou três estratégias:

| Opção | Descrição | Descartado por |
|-------|-----------|----------------|
| A | Autenticação embutida em cada microsserviço (sem serviço dedicado) | Duplicaria lógica de validação de senha/hash em todos os serviços |
| B | AWS Lambda + API Gateway + Cognito | Sem conta AWS disponível no escopo atual do projeto — o cluster é 100% local (Docker Desktop) |
| **C (escolhida)** | **Microsserviço `auth` dedicado (Spring Boot + JWT)**, rodando no mesmo cluster Kubernetes local dos demais serviços | Atende ao requisito, sem custo e sem dependência de nuvem |

---

## 3. Proposta

### 3.1 Componentes

```
Cliente → POST /auth/login (via Ingress NGINX) → auth-service:8090 → AuthController
                                                        ↓
                                          Busca usuário na tabela `users` (auth_db / PostgreSQL)
                                                        ↓
                                          Valida senha (hash bcrypt) via AuthenticationInputPort
                                                        ↓
                                          Gera JWT (HS256, claims: username, role)
                                                        ↓
                                    Retorna: { token, username, role }

Cliente → GET /auth/validate (Bearer <token>) → auth-service:8090
                                                        ↓
                                          JwtAuthenticationFilter valida assinatura + expiração
                                                        ↓
                                          Retorna: { valid, username, role }
```

Demais microsserviços (`video-upload`, `video-processing`, `video-status`, `video-download`, `notification`) recebem o mesmo `JWT_SECRET` via Kubernetes Secret (`jwt-secret`, publicado pelo `iac-terraform`) para validar o token localmente quando necessário, sem chamar de volta o serviço `auth` a cada requisição.

### 3.2 Regras de Validação de Identidade

1. Busca usuário pelo `username` na tabela `users` (`auth_db`);
2. Compara a senha informada com o hash armazenado (bcrypt) via `password_hash`;
3. Usuário possui uma `role` (`ADMIN` ou `USER`) e uma flag `active`;
4. Usuários de exemplo (seed via Flyway `V1__create_users_table.sql`): `admin`/`admin123` (ADMIN) e `user`/`user123` (USER) — apenas para ambiente local/demonstração.

### 3.3 Segredo JWT

- Variável de ambiente `JWT_SECRET`, sincronizada via Kubernetes Secret entre o serviço `auth` e os demais microsserviços que precisem validar o token;
- Algoritmo: **HS256**;
- Expiração configurável via `JWT_EXPIRATION_MS` (ConfigMap `auth-config`).

### 3.4 Rotas

| Grupo | Rotas | Autenticação |
|-------|-------|--------------|
| Públicas | `POST /auth/login` | Nenhuma |
| Protegidas | `GET /auth/validate` | JWT Bearer |
| Demais serviços | `/video-upload/**`, `/processing/**`, `/status/**`, `/download/**`, `/notify/**` | JWT Bearer (validação local em cada serviço ou via filtro compartilhado) |

---

## 4. Alternativas Rejeitadas

### 4.1 AWS Lambda + API Gateway + Cognito (arquitetura original, revogada)
- Adequada apenas em cenário com conta AWS ativa (ex.: AWS Academy de uma fase anterior do curso);
- O projeto atual não provisiona nenhum recurso AWS — toda a infraestrutura é local (Terraform + Kubernetes no Docker Desktop, ver `iac-terraform`);
- O código legado da Lambda foi mantido em `src/auth-lambda/` apenas como referência histórica, sem uso ativo.

### 4.2 Autenticação replicada em cada microsserviço, sem serviço dedicado
- Evitaria uma dependência de rede entre serviços;
- Rejeitada: duplicaria a lógica de hash de senha, emissão de token e schema de usuários em múltiplos repositórios, dificultando manutenção.

### 4.3 OAuth2 / OIDC externo (Auth0, Okta, Keycloak)
- Dependência de serviço externo (pago ou com setup adicional);
- Fora do escopo — o requisito do desafio é simples ("usuário e senha"), não exige federação de identidade externa.

---

## 5. Consequências

### Positivas
- Zero custo e zero dependência de nuvem — tudo roda no cluster Kubernetes local;
- Separação clara entre autenticação (`auth-service`) e os demais domínios de negócio;
- Reaproveita a mesma stack tecnológica (Spring Boot 3 / Java 21 / PostgreSQL / Flyway) usada nos outros microsserviços, reduzindo curva de aprendizado da equipe;
- Observability via New Relic e `/actuator/prometheus`, igual aos demais serviços.

### Negativas / Riscos
- Sem cache de autorização centralizado (como um API Gateway ofereceria) — cada validação de token é feita em tempo real;
- Usuários seed (`admin`/`user`) com senhas fixas em migration Flyway — aceitável apenas em ambiente local/demonstração, **não deve ser usado em produção real**.

---

## 6. Referências

- [RFC 7519 — JSON Web Token (JWT)](https://datatracker.ietf.org/doc/html/rfc7519)
- [RFC 7518 — JSON Web Algorithms (JWA)](https://datatracker.ietf.org/doc/html/rfc7518)
- `docs/requirements/POSTECH - SOAT - Fase 5 - Hacka.txt` (workspace raiz) — requisito de autenticação por usuário e senha.

**RFC relacionado:** RFC-003 e RFC-004 (`iac-terraform`) — infraestrutura Kubernetes local e arquitetura geral da solução.
