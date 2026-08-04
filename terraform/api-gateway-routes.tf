# =============================================================================
# API Gateway Routes — Fase 5
# Rotas publicas: /auth/*, /healthz, /*/swagger-ui*, /*/v3/api-docs*
# Rotas protegidas: endpoints de negócio de todos os MSs requerem JWT valido
#
# Arquitetura: cada MS tem seu proprio NLB listener (NodePort dedicado).
# O path rewriting nas integracoes strips o prefixo /<ms-name>/ antes de
# encaminhar para o pod, ex: GET /customer/api/customers -> GET /api/customers
# =============================================================================

locals {
  protected_authorization_type = var.enable_lambda_authorizer ? "CUSTOM" : "NONE"
  protected_authorizer_id      = var.enable_lambda_authorizer ? aws_apigatewayv2_authorizer.lambda_auth.id : null
}

# ─── Rotas publicas de autenticacao ──────────────────────────────────────────

resource "aws_apigatewayv2_route" "auth_post" {
  api_id    = aws_apigatewayv2_api.main.id
  route_key = "POST /auth/login"
  target    = "integrations/${aws_apigatewayv2_integration.lambda_login.id}"
}

resource "aws_apigatewayv2_route" "auth_proxy" {
  api_id    = aws_apigatewayv2_api.main.id
  route_key = "ANY /auth/{proxy+}"
  target    = "integrations/${aws_apigatewayv2_integration.lambda_login.id}"
}

# Health check publico - aponta para Lambda enquanto EKS pode nao ter servicos
resource "aws_apigatewayv2_route" "health_get" {
  api_id    = aws_apigatewayv2_api.main.id
  route_key = "GET /healthz"
  target    = "integrations/${aws_apigatewayv2_integration.lambda_login.id}"
}

# Rota publica de login via proxy no customer MS — sem JWT, necessario para Swagger UI
resource "aws_apigatewayv2_route" "customer_auth_login_public" {
  api_id    = aws_apigatewayv2_api.main.id
  route_key = "POST /customer/auth/login"
  target    = "integrations/${aws_apigatewayv2_integration.customer_auth_proxy.id}"
}

# ─── Rotas publicas Swagger (sem JWT) — Customer ─────────────────────────────

resource "aws_apigatewayv2_route" "customer_swagger_ui" {
  api_id    = aws_apigatewayv2_api.main.id
  route_key = "GET /customer/swagger-ui.html"
  target    = "integrations/${aws_apigatewayv2_integration.ms_swagger["customer"].id}"
}

resource "aws_apigatewayv2_route" "customer_swagger_proxy" {
  api_id    = aws_apigatewayv2_api.main.id
  route_key = "GET /customer/swagger-ui/{proxy+}"
  target    = "integrations/${aws_apigatewayv2_integration.ms_swagger["customer"].id}"
}

resource "aws_apigatewayv2_route" "customer_apidocs" {
  api_id    = aws_apigatewayv2_api.main.id
  route_key = "GET /customer/v3/api-docs"
  target    = "integrations/${aws_apigatewayv2_integration.ms_swagger["customer"].id}"
}

resource "aws_apigatewayv2_route" "customer_apidocs_proxy" {
  api_id    = aws_apigatewayv2_api.main.id
  route_key = "GET /customer/v3/api-docs/{proxy+}"
  target    = "integrations/${aws_apigatewayv2_integration.ms_swagger["customer"].id}"
}

# ─── Rotas publicas Swagger (sem JWT) — Vehicle ──────────────────────────────

resource "aws_apigatewayv2_route" "vehicle_swagger_ui" {
  api_id    = aws_apigatewayv2_api.main.id
  route_key = "GET /vehicle/swagger-ui.html"
  target    = "integrations/${aws_apigatewayv2_integration.ms_swagger["vehicle"].id}"
}

resource "aws_apigatewayv2_route" "vehicle_swagger_proxy" {
  api_id    = aws_apigatewayv2_api.main.id
  route_key = "GET /vehicle/swagger-ui/{proxy+}"
  target    = "integrations/${aws_apigatewayv2_integration.ms_swagger["vehicle"].id}"
}

resource "aws_apigatewayv2_route" "vehicle_apidocs" {
  api_id    = aws_apigatewayv2_api.main.id
  route_key = "GET /vehicle/v3/api-docs"
  target    = "integrations/${aws_apigatewayv2_integration.ms_swagger["vehicle"].id}"
}

resource "aws_apigatewayv2_route" "vehicle_apidocs_proxy" {
  api_id    = aws_apigatewayv2_api.main.id
  route_key = "GET /vehicle/v3/api-docs/{proxy+}"
  target    = "integrations/${aws_apigatewayv2_integration.ms_swagger["vehicle"].id}"
}

# ─── Rotas publicas Swagger (sem JWT) — Service ──────────────────────────────

resource "aws_apigatewayv2_route" "service_swagger_ui" {
  api_id    = aws_apigatewayv2_api.main.id
  route_key = "GET /service/swagger-ui.html"
  target    = "integrations/${aws_apigatewayv2_integration.ms_swagger["service"].id}"
}

resource "aws_apigatewayv2_route" "service_swagger_proxy" {
  api_id    = aws_apigatewayv2_api.main.id
  route_key = "GET /service/swagger-ui/{proxy+}"
  target    = "integrations/${aws_apigatewayv2_integration.ms_swagger["service"].id}"
}

resource "aws_apigatewayv2_route" "service_apidocs" {
  api_id    = aws_apigatewayv2_api.main.id
  route_key = "GET /service/v3/api-docs"
  target    = "integrations/${aws_apigatewayv2_integration.ms_swagger["service"].id}"
}

resource "aws_apigatewayv2_route" "service_apidocs_proxy" {
  api_id    = aws_apigatewayv2_api.main.id
  route_key = "GET /service/v3/api-docs/{proxy+}"
  target    = "integrations/${aws_apigatewayv2_integration.ms_swagger["service"].id}"
}

# ─── Rotas publicas Swagger (sem JWT) — Stocks ───────────────────────────────

resource "aws_apigatewayv2_route" "stocks_swagger_ui" {
  api_id    = aws_apigatewayv2_api.main.id
  route_key = "GET /stocks/swagger-ui.html"
  target    = "integrations/${aws_apigatewayv2_integration.ms_swagger["stocks"].id}"
}

resource "aws_apigatewayv2_route" "stocks_swagger_proxy" {
  api_id    = aws_apigatewayv2_api.main.id
  route_key = "GET /stocks/swagger-ui/{proxy+}"
  target    = "integrations/${aws_apigatewayv2_integration.ms_swagger["stocks"].id}"
}

resource "aws_apigatewayv2_route" "stocks_apidocs" {
  api_id    = aws_apigatewayv2_api.main.id
  route_key = "GET /stocks/v3/api-docs"
  target    = "integrations/${aws_apigatewayv2_integration.ms_swagger["stocks"].id}"
}

resource "aws_apigatewayv2_route" "stocks_apidocs_proxy" {
  api_id    = aws_apigatewayv2_api.main.id
  route_key = "GET /stocks/v3/api-docs/{proxy+}"
  target    = "integrations/${aws_apigatewayv2_integration.ms_swagger["stocks"].id}"
}

# ─── Rotas publicas Swagger (sem JWT) — Billing ──────────────────────────────

resource "aws_apigatewayv2_route" "billing_swagger_ui" {
  api_id    = aws_apigatewayv2_api.main.id
  route_key = "GET /billing/swagger-ui.html"
  target    = "integrations/${aws_apigatewayv2_integration.ms_swagger["billing"].id}"
}

resource "aws_apigatewayv2_route" "billing_swagger_proxy" {
  api_id    = aws_apigatewayv2_api.main.id
  route_key = "GET /billing/swagger-ui/{proxy+}"
  target    = "integrations/${aws_apigatewayv2_integration.ms_swagger["billing"].id}"
}

resource "aws_apigatewayv2_route" "billing_apidocs" {
  api_id    = aws_apigatewayv2_api.main.id
  route_key = "GET /billing/v3/api-docs"
  target    = "integrations/${aws_apigatewayv2_integration.ms_swagger["billing"].id}"
}

resource "aws_apigatewayv2_route" "billing_apidocs_proxy" {
  api_id    = aws_apigatewayv2_api.main.id
  route_key = "GET /billing/v3/api-docs/{proxy+}"
  target    = "integrations/${aws_apigatewayv2_integration.ms_swagger["billing"].id}"
}

# ─── Rotas publicas Swagger (sem JWT) — Purchase-Order ───────────────────────

resource "aws_apigatewayv2_route" "purchase_order_swagger_ui" {
  api_id    = aws_apigatewayv2_api.main.id
  route_key = "GET /purchase-order/swagger-ui.html"
  target    = "integrations/${aws_apigatewayv2_integration.ms_swagger["purchase"].id}"
}

resource "aws_apigatewayv2_route" "purchase_order_swagger_proxy" {
  api_id    = aws_apigatewayv2_api.main.id
  route_key = "GET /purchase-order/swagger-ui/{proxy+}"
  target    = "integrations/${aws_apigatewayv2_integration.ms_swagger["purchase"].id}"
}

resource "aws_apigatewayv2_route" "purchase_order_apidocs" {
  api_id    = aws_apigatewayv2_api.main.id
  route_key = "GET /purchase-order/v3/api-docs"
  target    = "integrations/${aws_apigatewayv2_integration.ms_swagger["purchase"].id}"
}

resource "aws_apigatewayv2_route" "purchase_order_apidocs_proxy" {
  api_id    = aws_apigatewayv2_api.main.id
  route_key = "GET /purchase-order/v3/api-docs/{proxy+}"
  target    = "integrations/${aws_apigatewayv2_integration.ms_swagger["purchase"].id}"
}

# ─── MS Customer — rotas protegidas ──────────────────────────────────────────

resource "aws_apigatewayv2_route" "customer_root" {
  api_id             = aws_apigatewayv2_api.main.id
  route_key          = "ANY /customer"
  target             = "integrations/${aws_apigatewayv2_integration.ms["customer"].id}"
  authorization_type = local.protected_authorization_type
  authorizer_id      = local.protected_authorizer_id
}

resource "aws_apigatewayv2_route" "customer_proxy" {
  api_id             = aws_apigatewayv2_api.main.id
  route_key          = "ANY /customer/{proxy+}"
  target             = "integrations/${aws_apigatewayv2_integration.ms["customer"].id}"
  authorization_type = local.protected_authorization_type
  authorizer_id      = local.protected_authorizer_id
}

# ─── MS Vehicle — rotas protegidas ───────────────────────────────────────────

resource "aws_apigatewayv2_route" "vehicle_root" {
  api_id             = aws_apigatewayv2_api.main.id
  route_key          = "ANY /vehicle"
  target             = "integrations/${aws_apigatewayv2_integration.ms["vehicle"].id}"
  authorization_type = local.protected_authorization_type
  authorizer_id      = local.protected_authorizer_id
}

resource "aws_apigatewayv2_route" "vehicle_proxy" {
  api_id             = aws_apigatewayv2_api.main.id
  route_key          = "ANY /vehicle/{proxy+}"
  target             = "integrations/${aws_apigatewayv2_integration.ms["vehicle"].id}"
  authorization_type = local.protected_authorization_type
  authorizer_id      = local.protected_authorizer_id
}

# ─── MS Service ──────────────────────────────────────────────────────────────

resource "aws_apigatewayv2_route" "service_root" {
  api_id             = aws_apigatewayv2_api.main.id
  route_key          = "ANY /service"
  target             = "integrations/${aws_apigatewayv2_integration.ms["service"].id}"
  authorization_type = local.protected_authorization_type
  authorizer_id      = local.protected_authorizer_id
}

resource "aws_apigatewayv2_route" "service_proxy" {
  api_id             = aws_apigatewayv2_api.main.id
  route_key          = "ANY /service/{proxy+}"
  target             = "integrations/${aws_apigatewayv2_integration.ms["service"].id}"
  authorization_type = local.protected_authorization_type
  authorizer_id      = local.protected_authorizer_id
}

# ─── MS Stocks ───────────────────────────────────────────────────────────────

resource "aws_apigatewayv2_route" "stocks_root" {
  api_id             = aws_apigatewayv2_api.main.id
  route_key          = "ANY /stocks"
  target             = "integrations/${aws_apigatewayv2_integration.ms["stocks"].id}"
  authorization_type = local.protected_authorization_type
  authorizer_id      = local.protected_authorizer_id
}

resource "aws_apigatewayv2_route" "stocks_proxy" {
  api_id             = aws_apigatewayv2_api.main.id
  route_key          = "ANY /stocks/{proxy+}"
  target             = "integrations/${aws_apigatewayv2_integration.ms["stocks"].id}"
  authorization_type = local.protected_authorization_type
  authorizer_id      = local.protected_authorizer_id
}

# ─── MS Billing ──────────────────────────────────────────────────────────────

resource "aws_apigatewayv2_route" "billing_root" {
  api_id             = aws_apigatewayv2_api.main.id
  route_key          = "ANY /billing"
  target             = "integrations/${aws_apigatewayv2_integration.ms["billing"].id}"
  authorization_type = local.protected_authorization_type
  authorizer_id      = local.protected_authorizer_id
}

resource "aws_apigatewayv2_route" "billing_proxy" {
  api_id             = aws_apigatewayv2_api.main.id
  route_key          = "ANY /billing/{proxy+}"
  target             = "integrations/${aws_apigatewayv2_integration.ms["billing"].id}"
  authorization_type = local.protected_authorization_type
  authorizer_id      = local.protected_authorizer_id
}

# Webhook do Mercado Pago — rota publica (sem JWT, autenticado pelo proprio MP)
resource "aws_apigatewayv2_route" "webhook_post" {
  api_id    = aws_apigatewayv2_api.main.id
  route_key = "POST /webhook"
  target    = "integrations/${aws_apigatewayv2_integration.ms["billing"].id}"
}

# ─── MS Purchase-Order ───────────────────────────────────────────────────────

resource "aws_apigatewayv2_route" "purchase_order_root" {
  api_id             = aws_apigatewayv2_api.main.id
  route_key          = "ANY /purchase-order"
  target             = "integrations/${aws_apigatewayv2_integration.ms["purchase"].id}"
  authorization_type = local.protected_authorization_type
  authorizer_id      = local.protected_authorizer_id
}

resource "aws_apigatewayv2_route" "purchase_order_proxy" {
  api_id             = aws_apigatewayv2_api.main.id
  route_key          = "ANY /purchase-order/{proxy+}"
  target             = "integrations/${aws_apigatewayv2_integration.ms["purchase"].id}"
  authorization_type = local.protected_authorization_type
  authorizer_id      = local.protected_authorizer_id
}

# Integração dedicada para service-order swagger (sem path stripping)
# O springdoc em modo dev serve em /service-order/* então o path deve ser passado integralmente
resource "aws_apigatewayv2_integration" "eks_so_swagger" {
  api_id                 = aws_apigatewayv2_api.main.id
  integration_type       = "HTTP_PROXY"
  integration_uri        = data.terraform_remote_state.infra.outputs.nlb_listener_arn
  integration_method     = "ANY"
  connection_type        = "VPC_LINK"
  connection_id          = aws_apigatewayv2_vpc_link.eks.id
  payload_format_version = "1.0"
  # Sem overwrite:path — springdoc serve em /service-order/* no perfil dev
}

# ─── Rotas publicas Swagger (sem JWT) — Service-Order ────────────────────────

resource "aws_apigatewayv2_route" "service_order_swagger_ui" {
  api_id    = aws_apigatewayv2_api.main.id
  route_key = "GET /service-order/swagger-ui.html"
  target    = "integrations/${aws_apigatewayv2_integration.eks_so_swagger.id}"
}

resource "aws_apigatewayv2_route" "service_order_swagger_proxy" {
  api_id    = aws_apigatewayv2_api.main.id
  route_key = "GET /service-order/swagger-ui/{proxy+}"
  target    = "integrations/${aws_apigatewayv2_integration.eks_so_swagger.id}"
}

resource "aws_apigatewayv2_route" "service_order_apidocs" {
  api_id    = aws_apigatewayv2_api.main.id
  route_key = "GET /service-order/v3/api-docs"
  target    = "integrations/${aws_apigatewayv2_integration.eks_so_swagger.id}"
}

resource "aws_apigatewayv2_route" "service_order_apidocs_proxy" {
  api_id    = aws_apigatewayv2_api.main.id
  route_key = "GET /service-order/v3/api-docs/{proxy+}"
  target    = "integrations/${aws_apigatewayv2_integration.eks_so_swagger.id}"
}

# Rota publica de login via proxy no service-order MS — sem JWT, necessario para Swagger UI
resource "aws_apigatewayv2_route" "service_order_auth_login_public" {
  api_id    = aws_apigatewayv2_api.main.id
  route_key = "POST /service-order/auth/login"
  target    = "integrations/${aws_apigatewayv2_integration.eks_so_swagger.id}"
}

# Rotas publicas de aprovacao/rejeicao via link de email — sem JWT
# Usa eks_so_swagger (sem path stripping): o path /service-order/api/v1/... chega intacto
# no Spring Boot onde o ApiPrefixStripFilter remove o prefixo /service-order antes do dispatch.
resource "aws_apigatewayv2_route" "service_order_approve_public" {
  api_id    = aws_apigatewayv2_api.main.id
  route_key = "GET /service-order/api/v1/service-orders/{serviceOrderId}/approve"
  target    = "integrations/${aws_apigatewayv2_integration.eks_so_swagger.id}"
}

resource "aws_apigatewayv2_route" "service_order_reject_public" {
  api_id    = aws_apigatewayv2_api.main.id
  route_key = "GET /service-order/api/v1/service-orders/{serviceOrderId}/reject"
  target    = "integrations/${aws_apigatewayv2_integration.eks_so_swagger.id}"
}

# POST — submit do formulario HTML de aprovacao/rejeicao (sem JWT; autenticado via token na query string)
resource "aws_apigatewayv2_route" "service_order_approve_post_public" {
  api_id    = aws_apigatewayv2_api.main.id
  route_key = "POST /service-order/api/v1/service-orders/{serviceOrderId}/approve"
  target    = "integrations/${aws_apigatewayv2_integration.eks_so_swagger.id}"
}

resource "aws_apigatewayv2_route" "service_order_reject_post_public" {
  api_id    = aws_apigatewayv2_api.main.id
  route_key = "POST /service-order/api/v1/service-orders/{serviceOrderId}/reject"
  target    = "integrations/${aws_apigatewayv2_integration.eks_so_swagger.id}"
}

# Integracoes legadas — mantidas para evitar conflito de estado no Terraform.
# Nao sao referenciadas por nenhuma rota; podem ser removidas num futuro cleanup.
resource "aws_apigatewayv2_integration" "so_approve" {
  api_id                 = aws_apigatewayv2_api.main.id
  integration_type       = "HTTP_PROXY"
  integration_uri        = data.terraform_remote_state.infra.outputs.nlb_listener_arn
  integration_method     = "ANY"
  connection_type        = "VPC_LINK"
  connection_id          = aws_apigatewayv2_vpc_link.eks.id
  payload_format_version = "1.0"
}

resource "aws_apigatewayv2_integration" "so_reject" {
  api_id                 = aws_apigatewayv2_api.main.id
  integration_type       = "HTTP_PROXY"
  integration_uri        = data.terraform_remote_state.infra.outputs.nlb_listener_arn
  integration_method     = "ANY"
  connection_type        = "VPC_LINK"
  connection_id          = aws_apigatewayv2_vpc_link.eks.id
  payload_format_version = "1.0"
}

# ─── MS Service-Order (principal) ────────────────────────────────────────────

resource "aws_apigatewayv2_route" "service_order_root" {
  api_id             = aws_apigatewayv2_api.main.id
  route_key          = "ANY /service-order"
  target             = "integrations/${aws_apigatewayv2_integration.eks.id}"
  authorization_type = local.protected_authorization_type
  authorizer_id      = local.protected_authorizer_id
}

resource "aws_apigatewayv2_route" "service_order_proxy" {
  api_id             = aws_apigatewayv2_api.main.id
  route_key          = "ANY /service-order/{proxy+}"
  target             = "integrations/${aws_apigatewayv2_integration.eks.id}"
  authorization_type = local.protected_authorization_type
  authorizer_id      = local.protected_authorizer_id
}

# ─── Catch-all protegido para demais rotas ────────────────────────────────────

resource "aws_apigatewayv2_route" "public_catchall" {
  api_id             = aws_apigatewayv2_api.main.id
  route_key          = "ANY /{proxy+}"
  target             = "integrations/${aws_apigatewayv2_integration.eks.id}"
  authorization_type = local.protected_authorization_type
  authorizer_id      = local.protected_authorizer_id
}