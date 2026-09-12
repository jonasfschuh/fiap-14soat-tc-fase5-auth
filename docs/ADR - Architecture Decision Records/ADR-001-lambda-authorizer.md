# ADR-001 — Autenticação JWT embutida no serviço `auth` (Spring Security), sem AWS Lambda

| Campo        | Valor                                                                     |
|--------------|-----------------------------------------------------------------------------|
| **ADR**      | 001                                                                       |
| **Título**   | Filtro Spring Security (`JwtAuthenticationFilter`) como mecanismo de autenticação/autorização |
| **Repositório** | fiap-14soat-tc-fase5-auth                                                |
| **Status**   | Aceito — **revisado em 2026-09-10** (substitui a versão anterior baseada em AWS Lambda) |
| **Data**     | 2026-04-20 (criado) · 2026-09-10 (revisado para a realidade 100% local)   |
| **Decisores**| Time FIAP 14SOAT Fase 5                                                   |

---

## ⚠️ Nota sobre a revisão

A versão original deste ADR descrevia um **Lambda Authorizer da AWS** validando tokens no **API Gateway**. Essa arquitetura **não reflete mais o projeto**: não há conta AWS, API Gateway ou Cognito envolvidos. Toda a plataforma roda em um **cluster Kubernetes local (Docker Desktop)**, provisionado via Terraform (`fiap-14soat-tc-fase5-iac-terraform`), com **NGINX Ingress** como único ponto de entrada (ver ADR-003 do `iac-terraform`).

O código Java do antigo Lambda Authorizer ainda existe no repositório em `src/auth-lambda/`, mas está marcado explicitamente no seu próprio `README.md` como **"legado — mantido para referência histórica"** e não é mais implantado nem invocado por nenhum componente ativo da solução.

---

## Contexto

O serviço `auth` (Spring Boot 3 / Java 21) precisa validar credenciais e emitir/validar tokens JWT para proteger as rotas dos demais microsserviços (`video-upload`, `video-processing`, `video-status`, `video-download`, `notification`), todos expostos atrás do mesmo Ingress NGINX no cluster local.

As opções avaliadas para validar o token em cada requisição foram:

1. **Filtro Spring Security dentro de cada microsserviço** (cada serviço valida o JWT localmente);
2. **Filtro Spring Security centralizado apenas no serviço `auth`**, que expõe `/login` e `/validate`, delegando a validação a quem consumir o endpoint;
3. ~~AWS Lambda Authorizer + API Gateway~~ — descartado por não existir mais infraestrutura AWS no projeto.

---

## Decisão

**Implementar um `JwtAuthenticationFilter` (`OncePerRequestFilter`) dentro do próprio serviço `auth`**, registrado na cadeia do Spring Security (`SecurityConfiguration`), responsável por:

- Extrair o header `Authorization: Bearer <token>`;
- Validar a assinatura HS256 do JWT via `JwtTokenAdapter`;
- Popular o `SecurityContextHolder` com o `username` e a `role` extraídos das claims do token;
- Rejeitar com `401` requisições com token ausente/inválido nas rotas protegidas.

O serviço expõe dois endpoints REST principais:
- `POST /login` — autentica usuário/senha e retorna `{ token, username, role }`;
- `GET /validate` — valida o token do header `Authorization` e retorna `{ valid, username, role }` (usado por outros serviços/gateway para checagem pontual, se necessário).

O serviço roda como um `Deployment` Kubernetes (`k8s/deployment.yaml`), exposto internamente na porta `8090`, atrás do path `/auth` no Ingress NGINX do cluster local.

---

## Justificativa

| Critério | Filtro Spring Security no `auth` (escolhido) | AWS Lambda Authorizer (descartado) |
|----------|-----------------------------------------------|--------------------------------------|
| Dependência de nuvem | Nenhuma — roda 100% local no cluster Docker Desktop | Exigiria conta AWS, API Gateway e Cognito/CloudWatch |
| Custo | Zero | Free tier Lambda, mas inexistente no escopo atual (sem AWS) |
| Latência | Sem cold start — processo Java já ativo no Pod | Cold start Java ~2-5s na primeira invocação |
| Simplicidade de debug | Logs locais via `kubectl logs` / New Relic | Exigiria CloudWatch Logs |
| Alinhamento com o requisito do desafio | Atende (`"Sistema deve ser protegido por usuário e senha"`) sem exigir nuvem | Não aplicável — desafio não exige AWS |

O filtro Spring Security embutido foi escolhido porque:
- Elimina qualquer dependência de infraestrutura de nuvem paga ou específica de fornecedor;
- Mantém o fluxo de autenticação simples e testável localmente (`docker-compose`, `k8s` local);
- Reaproveita a mesma stack (Spring Boot 3 / Java 21) usada nos demais microsserviços da solução;
- É compatível com o requisito técnico do Hackathon de que o sistema seja "protegido por usuário e senha", sem exigir nenhum serviço gerenciado de nuvem.

---

## Consequências

### Positivas
- Zero dependência de AWS — toda a autenticação roda no cluster local;
- Sem cold start — validação de token é imediata dentro do próprio processo Spring Boot;
- Reaproveita `JWT_SECRET` compartilhado via Kubernetes Secret (`jwt-secret`) entre o serviço `auth` e os demais microsserviços que também precisem validar o token;
- Testes unitários e de integração do filtro rodam localmente sem mocks de AWS SDK.

### Negativas / Riscos
- Cada microsserviço que precisa validar o JWT deve implementar (ou compartilhar) sua própria lógica de validação — não há um único ponto centralizado de autorização como um API Gateway faria;
- Sem TTL de cache de autorização como o Lambda Authorizer oferecia (`authorizer_result_ttl_in_seconds`) — cada requisição revalida o token;
- Sem WAF/API Gateway na frente, a superfície de proteção fica no NGINX Ingress + validação de aplicação.

---

## Alternativas Consideradas

### AWS Lambda Authorizer + API Gateway (arquitetura original, revogada)
- Adequada em um cenário com conta AWS ativa (ex.: AWS Academy);
- Sem essa infraestrutura disponível no projeto atual, tornou-se inviável e foi substituída;
- Código mantido em `src/auth-lambda/` apenas como referência histórica, sem uso ativo.

### Validação de JWT replicada em cada microsserviço (sem endpoint central `/validate`)
- Evitaria uma chamada de rede extra ao serviço `auth`;
- Rejeitada por hora: manter o `auth` como fonte única de emissão/validação simplifica a rotação de segredo (`JWT_SECRET`) e a auditoria de login.

---

## Notas de Implementação

```java
// JwtAuthenticationFilter.java — infrastructure/adapters/security
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) {
        String token = extrairBearer(request.getHeader("Authorization"));
        if (!jwtTokenAdapter.validate(token)) {
            response.sendError(SC_UNAUTHORIZED, "Token inválido.");
            return;
        }
        // popula SecurityContextHolder com username + role extraídos do token
        chain.doFilter(request, response);
    }
}
```

- Porta do serviço: `8090` (ver `k8s/deployment.yaml`, `infra/ingress.tf` no `iac-terraform`);
- Segredo `JWT_SECRET` publicado como Kubernetes Secret (`jwt-secret`) pelo `iac-terraform` e consumido também pelos demais microsserviços;
- Banco de dados dedicado `auth_db` (Flyway) — ver ADR-004 do `iac-terraform`.

**ADR relacionado:** ADR-003 (Ingress NGINX local) e ADR-004 (Banco de dados local por serviço), ambos em `iac-terraform`.
