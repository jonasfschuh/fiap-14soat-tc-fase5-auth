# ADR-001 — Uso de Lambda Authorizer para Proteção de Rotas no API Gateway

| Campo        | Valor                                                          |
|--------------|----------------------------------------------------------------|
| **ADR**      | 001                                                            |
| **Título**   | Lambda Authorizer como mecanismo de autorização no API Gateway |
| **Repositório** | fiap-14soat-tc-fase5-auth-lambda                               |
| **Status**   | Aceito                                                         |
| **Data**     | 2026-04-20                                                     |
| **Decisores**| Time FIAP 14SOAT Fase 5                                       |

---

## Contexto

O API Gateway HTTP API v2 precisa de um mecanismo para validar tokens JWT antes de encaminhar requisições ao cluster EKS. As opções disponíveis no API Gateway são:

1. **JWT Authorizer nativo** (built-in do API Gateway);
2. **Lambda Authorizer** (função customizada);
3. **Sem autorização** (authorization_type = NONE).

Adicionalmente, a equipe já precisava de uma Lambda para o fluxo de **login** (geração de JWT), tornando vantajoso centralizar autenticação e autorização no mesmo repositório serverless.

---

## Decisão

**Utilizar Lambda Authorizer customizado** (`JwtAuthorizerHandler` em Java 21) para validar tokens JWT em todas as rotas protegidas do API Gateway.

---

## Justificativa

| Critério | JWT Authorizer nativo | Lambda Authorizer (escolhido) |
|----------|----------------------|-------------------------------|
| Controle sobre validação | Limitado (apenas issuer/audience) | Total (claims, role, expiração customizada) |
| Lógica de negócio | Não suporta | Suporta (ex: verificar se usuário ainda existe no banco) |
| Performance | Melhor (sem cold start) | Cold start Java ~2s (mitigável com SnapStart) |
| Custo | Sem custo adicional | Invocações Lambda (free tier cobre o volume acadêmico) |
| Cache de autorização | Sim (TTL configurável) | Sim (`authorizer_result_ttl_in_seconds = 300`) |

O Lambda Authorizer foi escolhido pois:

- Permite validar a **assinatura HS256** com o mesmo segredo usado pelo Spring Boot;
- Permite verificar **claims customizados** como `role` e `iss`;
- Facilita **evolução futura** (ex: revogar tokens, checar blacklist);
- Mantém consistência com o issuer `RaceforceApi` definido no `JWT_SECRET` compartilhado.

---

## Consequências

### Positivas
- Validação JWT centralizada e desacoplada da aplicação;
- TTL de 300s no cache do Authorizer reduz invocações repetidas;
- Logs estruturados no CloudWatch e New Relic para cada tentativa de autorização.

### Negativas
- Cold start Java 21 pode adicionar latência na primeira invocação após inatividade;
- O `ANY /{proxy+}` (catch-all) tem `authorization_type = NONE` — rotas não mapeadas explicitamente ficam abertas. **Ação futura:** adicionar rotas restantes ao conjunto protegido.

---

## Alternativas Consideradas

### JWT Authorizer nativo
- Não suporta segredo HS256 compartilhado diretamente (usa JWKS endpoint);
- Sem flexibilidade para adicionar lógica de negócio futura.

### Sem autorização (NONE em todas as rotas)
- Inaceitável: exporia dados sensíveis de clientes e ordens de serviço publicamente.

---

## Notas de Implementação

```java
// JwtAuthorizerHandler.java
public APIGatewayV2CustomAuthorizerResponse handleRequest(
    APIGatewayV2CustomAuthorizerEvent event, Context context) {
    String token = extrairBearer(event.getHeaders().get("authorization"));
    boolean valido = jwtService.validar(token);
    return buildResponse(valido, extrairSub(token));
}
```

- Handler: `br.com.fiap.authlambda.handler.JwtAuthorizerHandler::handleRequest`
- Runtime: Java 21
- TTL cache: 300s
- Simple responses habilitado (`enable_simple_responses = true`)

