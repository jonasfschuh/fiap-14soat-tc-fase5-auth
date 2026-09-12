# fiap-14soat-tc-fase5-auth

![Java 21](https://img.shields.io/badge/Java_21-%23ED8B00.svg?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot_3-%236DB33F.svg?style=for-the-badge&logo=springboot&logoColor=white)
![JWT](https://img.shields.io/badge/JWT_HS256-%23000000.svg?style=for-the-badge&logo=jsonwebtokens&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL_16-%23316192.svg?style=for-the-badge&logo=postgresql&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-%232496ED.svg?style=for-the-badge&logo=docker&logoColor=white)
![Kubernetes](https://img.shields.io/badge/Kubernetes-%23326CE5.svg?style=for-the-badge&logo=kubernetes&logoColor=white)
![Flyway](https://img.shields.io/badge/Flyway-%23CC0200.svg?style=for-the-badge&logo=flyway&logoColor=white)
![New Relic](https://img.shields.io/badge/New_Relic-%231CE783.svg?style=for-the-badge&logo=newrelic&logoColor=white)
![Zero Trust](https://img.shields.io/badge/Zero_Trust-Auth-1A1A2E?style=for-the-badge)
![DDD](https://img.shields.io/badge/Domain--Driven_Design-430098?style=for-the-badge)
![JUnit 5](https://img.shields.io/badge/JUnit_5-%2325A162.svg?style=for-the-badge&logo=junit5&logoColor=white)
![GitHub Actions](https://img.shields.io/badge/GitHub_Actions-%232671E5.svg?style=for-the-badge&logo=githubactions&logoColor=white)
![Maven](https://img.shields.io/badge/Apache_Maven-%23C71A36.svg?style=for-the-badge&logo=apachemaven&logoColor=white)

[![PR Validation](https://github.com/jonasfschuh/fiap-14soat-tc-fase5-auth-lambda/actions/workflows/pr-validation.yaml/badge.svg?branch=main)](https://github.com/jonasfschuh/fiap-14soat-tc-fase5-auth-lambda/actions/workflows/pr-validation.yaml)

---

## 📑 Sumário

- [👤 Autor](#-autor)
- [📋 Descrição](#-descrição)
- [🏗️ Arquitetura](#️-arquitetura)
- [🛠️ Tecnologias Utilizadas](#️-tecnologias-utilizadas)
- [⚙️ Variáveis de Configuração](#️-variáveis-de-configuração)
- [🔒 Proteção da Branch main](#-proteção-da-branch-main)
- [🚀 Execução e Deploy](#-execução-e-deploy)
- [🔐 Observações de Segurança](#-observações-de-segurança)
- [📈 Observabilidade](#-observabilidade)
- [📖 Documentação Técnica](#-documentação-técnica)
- [🎬 Vídeos de Apresentação](#-vídeos-de-apresentação)
- [🔗 Repositórios Relacionados](#-repositórios-relacionados)

---

## 👤 Autor

| Nome                 | E-mail                  | RM        | Discord          | WhatsApp        |
|----------------------|-------------------------|-----------|------------------|-----------------|
| Jonas Fernando Schuh | jonasschuh@hotmail.com  | rm369458  | jonasf.schuh     | 47 9 9960-1396  |

**Grupo:** 2 · FIAP 14SOAT Fase 5 — RaceForce

---

## 📋 Descrição

Este repositório implementa a **camada de autenticação** da plataforma RaceForce como uma aplicação **Spring Boot 3** (Java 21), executada localmente via Docker Compose ou dentro de um **cluster Kubernetes local**.

Expõe dois endpoints principais:

| Endpoint | Responsabilidade |
|----------|-----------------|
| `POST /auth/login` | Autentica usuário por CPF/senha no PostgreSQL e emite token **JWT HS256** |
| `GET /healthz` | Health check da aplicação |

### Regras de Autenticação

1. Busca o usuário na tabela `users` (administradores e operadores);
2. Se não encontrado, busca na tabela `service_order` pelo **CPF do cliente**;
3. Senha padrão para clientes = **primeiros 3 dígitos do CPF**;
4. Retorna role `ADMIN` para usuários administrativos ou `USER` para demais.

### Rotas

| Grupo | Rotas | Autenticação |
|-------|-------|--------------|
| Públicas | `POST /auth/login`, `GET /healthz` | Nenhuma |
| Protegidas | `/customer`, `/vehicle`, `/service`, `/product`, `GET /service-order` | **JWT Bearer** |

> ℹ️ Este repositório **não** inclui a aplicação principal Spring Boot, o banco de dados nem a infraestrutura Kubernetes dos demais microserviços — cada um possui seu próprio repositório (ver seção [Repositórios Relacionados](#-repositórios-relacionados)).

---

## 🏗️ Arquitetura

### Diagrama de Sequência — Fluxo de Autenticação

O fluxo completo (login → validação → emissão do JWT) corresponde às etapas 01 a 03 do diagrama de fluxo processual consolidado no repositório de infraestrutura:

![Fluxo Processual](https://raw.githubusercontent.com/jonasfschuh/fiap-14soat-tc-fase5-iac-terraform/main/docs/diagrams/process-flow.png)

### Fluxo de Login

```
Cliente
  │
  ▼  POST /auth/login  {cpf, senha}
Auth Service (Spring Boot :8090)
  │  SELECT users / service_order (PostgreSQL)
  │  validação BCrypt
  ▼
JWT HS256  {sub, role, email, iss, iat, exp}
  │
  ▼
{ token, role, expiresIn, username }  →  Cliente
```

### Fluxo de Autorização de Rotas Protegidas

```
Cliente
  │
  ▼  GET /video-upload   Authorization: Bearer <token>
Microserviço / Ingress
  │
  ▼
Auth Service: validação JWT
  │  valida assinatura HS256 + claims (iss, exp, role)
  ├── token válido   →  encaminha para o microserviço destino
  └── token inválido →  401 Unauthorized
```

---

## 🛠️ Tecnologias Utilizadas

| Tecnologia | Versão / Uso |
|------------|--------------|
| **Java 21** | Runtime da aplicação (LTS) |
| **Spring Boot 3** | Framework principal — web, security, data JPA, actuator |
| **Maven 3.9+** | Build e empacotamento (`maven-shade-plugin`) |
| **JWT (HS256)** | Tokens de autenticação — issuer `auth-service`, expiração 24h |
| **BCrypt** | Hash de senhas armazenadas no PostgreSQL |
| **PostgreSQL 16** | Fonte de dados para autenticação (tabelas `users` / `service_order`) |
| **Flyway** | Migração e versionamento do schema do banco |
| **Docker / Docker Compose** | Containerização e execução local |
| **Kubernetes (local)** | Orquestração via manifests em `k8s/` (namespace `fiapx`) |
| **New Relic** | Observabilidade: logs estruturados JSON + métricas customizadas |
| **GitHub Actions** | Pipeline CI/CD de build e validação automatizados |

---

## ⚙️ Variáveis de Configuração

### Variáveis de Ambiente

| Variável | Descrição | Exemplo |
|----------|-----------|---------|
| `JWT_SECRET` | Chave secreta HS256 (mínimo 32 chars) | `minha-chave-secreta-...` |
| `JWT_ISSUER` | Emissor do token JWT | `auth-service` |
| `JWT_EXPIRATION_MS` | Expiração do token em ms | `86400000` (24h) |
| `SPRING_DATASOURCE_URL` | JDBC URL do PostgreSQL | `jdbc:postgresql://host:5432/auth_db` |
| `SPRING_DATASOURCE_USERNAME` | Usuário do banco | `postgres` |
| `SPRING_DATASOURCE_PASSWORD` | Senha do banco | *(via secret k8s / .env)* |
| `SERVER_PORT` | Porta HTTP da aplicação | `8090` |
| `SPRING_PROFILES_ACTIVE` | Perfil ativo | `docker` ou `k8s` |

### Docker Compose — `.env`

```dotenv
JWT_SECRET=dev-secret-change-in-production-min-32-chars
```

> O `docker-compose.yml` sobe automaticamente o PostgreSQL (`postgres-auth:5432`) e o `auth-api` na porta `8090`.

### Kubernetes — ConfigMap e Secrets

| Recurso | Chave | Descrição |
|---------|-------|-----------|
| ConfigMap `auth-config` | `SERVER_PORT`, `JWT_ISSUER`, `JWT_EXPIRATION_MS`, `SPRING_*`, `LOGGING_*` | Configurações não-sensíveis |
| Secret `jwt-secret` | `JWT_SECRET` | Chave de assinatura JWT |
| Secret `postgres-auth-secret` | `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` | Credenciais do banco |

### Secrets para CI/CD (GitHub Actions)

| Secret / Variable | Descrição |
|-------------------|-----------|
| `TF_VAR_JWT_KEY` | Chave secreta do JWT |
| `TF_VAR_DB_PASSWORD` | Senha do PostgreSQL |
| `NEW_RELIC_KEY` | Chave de licença New Relic |
| `NEW_RELIC_ACCOUNT_ID` | Account ID New Relic |

---

## 🔒 Proteção da Branch main

As regras abaixo foram aplicadas em todos os repositórios da stack para atender ao requisito do Tech Challenge:

> *"Branch main protegida (sem commits diretos). Uso obrigatório de Pull Requests para merge."*

| Regra | Valor |
|---|---|
| **Require a pull request before merging** | ✅ Ativado — bloqueia commits diretos na `main` |
| **Required approvals** | `1` revisão obrigatória antes do merge (OBS: desabilitado neste estudo com 1 pessoa no grupo) |
| **Dismiss stale reviews on new commits** | ✅ Ativado — revalida aprovação se o PR for atualizado |
| **Require status checks to pass** | ✅ Ativado — bloqueia merge se o PR Validation falhar |
| **Require branches to be up to date** | ✅ Ativado — evita merge de branch desatualizada |
| **Do not allow bypassing** | ✅ Ativado — nem o owner ignora as regras |

### Status checks obrigatórios

| Check | Job |
|---|---|
| `build-and-test` | Build Maven + testes unitários (`./mvnw verify`) |

> ⚠️ O status check só aparece para seleção no GitHub após a **primeira execução bem-sucedida** do PR Validation.

---

## 🚀 Execução e Deploy

### Pré-requisitos

- [Java 21](https://adoptium.net/)
- [Maven 3.9+](https://maven.apache.org/)
- [Docker](https://www.docker.com/) e Docker Compose
- [kubectl](https://kubernetes.io/docs/tasks/tools/) (para deploy em k8s local)

### Opção A — Docker Compose (desenvolvimento local)

```bash
# Crie o arquivo .env (ou edite o valor padrão no docker-compose.yml)
cp .env.example .env

# Suba PostgreSQL + auth-api
docker compose up --build
```

A API ficará disponível em `http://localhost:8090`.

### Opção B — Kubernetes local

```bash
# Build da imagem local (sem push para registry)
docker build -t auth-api:latest .

# Crie os secrets antes de aplicar os manifests
kubectl create secret generic jwt-secret \
  --from-literal=JWT_SECRET=<sua-chave-secreta> \
  -n fiapx

kubectl create secret generic postgres-auth-secret \
  --from-literal=SPRING_DATASOURCE_URL=jdbc:postgresql://<host>:5432/auth_db \
  --from-literal=SPRING_DATASOURCE_USERNAME=postgres \
  --from-literal=SPRING_DATASOURCE_PASSWORD=<senha> \
  -n fiapx

# Aplique os manifests
kubectl apply -f k8s/
```

O serviço fica exposto como `ClusterIP` na porta `8090` dentro do namespace `fiapx`.

### Testes Unitários

```bash
./mvnw clean test
```

### Testes com cobertura completa

```bash
./mvnw verify
```

### Ordem de Deploy da Stack Completa (k8s local)

| # | Repositório | Descrição |
|---|-------------|-----------|
| 1 | fiap-14soat-tc-fase5-iac-terraform | VPC, EKS, NLB — infraestrutura base |
| 2 | fiap-14soat-tc-fase5-iac-database | PostgreSQL |
| 3 | **fiap-14soat-tc-fase5-auth** ← este | Auth Service Spring Boot |
| 4 | fiap-14soat-tc-fase5-app-k8s | Demais microserviços + manifests K8s |

---

## 🔐 Observações de Segurança

- **Nunca** comite `JWT_SECRET` ou `SPRING_DATASOURCE_PASSWORD` em texto puro no repositório;
- Em k8s, injete segredos via `kubectl create secret` ou um gerenciador de secrets (ex.: Sealed Secrets, Vault);
- O arquivo `.env` está no `.gitignore` — use `.env.example` como referência;
- Rotas sensíveis devem ser explicitamente declaradas como protegidas no filtro JWT da aplicação.

> 💡 O ambiente pode conter o usuário de teste `admin / admin123` exclusivamente para validação do Tech Challenge. **Remova em produção.**

---

## 📈 Observabilidade

Logs estruturados JSON e métricas via Spring Boot Actuator + Prometheus em cada requisição de autenticação:

| Campo | Descrição |
|-------|-----------|
| `cpf` | CPF do usuário autenticado (mascarado) |
| `authResult` | `SUCCESS` ou `FAILURE` |
| `reason` | Motivo da falha (quando aplicável) |
| `latencyMs` | Latência da operação |
| `correlationId` | Header `X-Correlation-Id` da requisição |

**Endpoints de Actuator expostos:**

| Endpoint | Descrição |
|----------|-----------|
| `GET /actuator/health` | Health check geral |
| `GET /actuator/health/liveness` | Liveness probe (k8s) |
| `GET /actuator/health/readiness` | Readiness probe (k8s) |
| `GET /actuator/prometheus` | Métricas no formato Prometheus |

**Métricas customizadas** no namespace `Raceforce/Auth`:

| Métrica | Descrição |
|---------|-----------|
| `auth_login_success_total` | Total de logins bem-sucedidos |
| `auth_login_failure_total` | Total de logins falhos (com dimensão `reason`) |
| `auth_token_issued_total` | Total de tokens JWT emitidos |

---

## 📖 Documentação Técnica

| Tipo | Arquivo | Descrição |
|------|---------|-----------|
| 📋 RFC | [RFC-001 — Autenticação com JWT](docs/RFC%20-%20Requests%20for%20Comments/RFC-001-autenticacao-serverless.md) | Justificativa técnica da estratégia de autenticação local (Spring Boot + JWT), substitui a proposta anterior baseada em AWS Lambda |
| 🏛️ ADR | [ADR-001 — Authorizer para Proteção de Rotas](docs/ADR%20-%20Architecture%20Decision%20Records/ADR-001-lambda-authorizer.md) | Decisão arquitetural: validação JWT no filtro Spring Security (`JwtAuthenticationFilter`), sem AWS Lambda |

> ℹ️ O diagrama de sequência do fluxo de autenticação (Cliente → Auth Service → PostgreSQL → JWT) faz parte da documentação consolidada de arquitetura no repositório de infraestrutura: veja [`process-flow.png`](https://github.com/jonasfschuh/fiap-14soat-tc-fase5-iac-terraform/blob/main/docs/diagrams/process-flow.png) e o [RFC-004 — Arquitetura Geral e Fluxo de Mensageria](https://github.com/jonasfschuh/fiap-14soat-tc-fase5-iac-terraform/blob/main/docs/RFC%20-%20Requests%20for%20Comments/RFC-004-arquitetura-geral-e-fluxo-de-mensageria.md).

> ℹ️ **Swagger / OpenAPI:** a documentação interativa está disponível em `http://localhost:8090/swagger-ui.html` com a aplicação em execução.

### Endpoints da API

#### `POST /auth/login`

```json
// Request
{
  "cpf": "12345678900",
  "senha": "123"
}

// Response 200
{
  "token": "<jwt>",
  "role": "USER",
  "expiresIn": 86400000,
  "username": "João Silva"
}

// Response 401
{
  "message": "Credenciais inválidas"
}
```

#### `GET /healthz`

```json
// Response 200
{
  "status": "UP"
}
```

---

## 🎬 Vídeos de Apresentação

| Fase | Link |
|------|------|
| Fase 1 | [Apresentação Tech Challenge 1 — RaceForce](https://youtu.be/EKwE8l4yE1M) |
| Fase 2 | [Apresentação Tech Challenge 2 — RaceForce](https://youtu.be/95ml0-H9Vf4) |
| Fase 3 | [Apresentação Tech Challenge 3 — RaceForce](https://www.youtube.com/watch?v=KB-FC_4zsPE) |
| Fase 4 | [Apresentação Tech Challenge 4 — RaceForce](https://www.youtube.com/watch?v=vR3x4kW0l90) |
| Fase 5 | [Apresentação Tech Challenge 5 — RaceForce](https://youtu.be/PFs5ekuUxxM) |

---

## 🔗 Repositórios Relacionados

| Ordem | Repositório | Descrição                   |
|-------|-------------|-----------------------------|
| 1 | [fiap-14soat-tc-fase5-iac-terraform](https://github.com/jonasfschuh/fiap-14soat-tc-fase5-iac-terraform) | Banco de dados, RabbitMQ — infraestrutura AWS |
| 2 | [fiap-14soat-tc-fase5-auth](https://github.com/jonasfschuh/fiap-14soat-tc-fase5-auth) | Login Authorizer            |
| 3 | [fiap-14soat-tc-fase5-video-upload-service](https://github.com/jonasfschuh/fiap-14soat-tc-fase5-video-upload-service) | Upload + RabbitMQ publisher |
| 4 | [fiap-14soat-tc-fase5-video-processing-service](https://github.com/jonasfschuh/fiap-14soat-tc-fase5-video-processing-service) | Processa vídeo, extrai frames, gera ZIP |
| 5 | [fiap-14soat-tc-fase5-video-status-service](https://github.com/jonasfschuh/fiap-14soat-tc-fase5-video-status-service) | Status e metadados dos vídeos por usuário |
| 6 | [fiap-14soat-tc-fase5-video-download-service](https://github.com/jonasfschuh/fiap-14soat-tc-fase5-video-download-service) | Download do ZIP via presigned URL |
| 7 | [fiap-14soat-tc-fase5-notification-service](https://github.com/jonasfschuh/fiap-14soat-tc-fase5-notification-service) | Notificação por e-mail em caso de erro/conclusão |
| 8 | [fiap-14soat-tc-fase5-observability](https://github.com/jonasfschuh/fiap-14soat-tc-fase5-observability) | Prometheus + Grafana — dashboards e alertas |

---

<div align="center">

**🎓 Desenvolvido para o Tech Challenge FIAP 14SOAT — Fase 5**

*Projeto Acadêmico — Pós-Graduação em Arquitetura de Software · FIAP 2025/2026*

[⬆ Voltar ao topo](#fiap-14soat-tc-fase5-auth)

</div>