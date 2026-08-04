# Data source para obter informações da infraestrutura (VPC, NLB, etc.)
data "terraform_remote_state" "infra" {
  backend = "s3"
  config = {
    bucket = var.infra_terraform_state_bucket
    key    = "infra/terraform.tfstate"
    region = var.aws_region
  }
}

# API Gateway HTTP API
resource "aws_apigatewayv2_api" "main" {
  name          = "${var.project_identifier}-api-gateway"
  protocol_type = "HTTP"
  description   = "API Gateway para roteamento entre Lambda (auth) e EKS (aplicação)"

  cors_configuration {
    allow_origins  = ["*"]
    allow_methods  = ["GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"]
    allow_headers  = ["content-type", "authorization", "x-api-key"]
    expose_headers = ["content-type"]
    max_age        = 3600
  }

  tags = {
    Name = "${var.project_identifier}-api-gateway"
  }
}

# CloudWatch Log Group para API Gateway
resource "aws_cloudwatch_log_group" "api_gateway" {
  name              = "/aws/apigateway/${var.project_identifier}-api-gateway"
  retention_in_days = 7

  tags = {
    Name = "${var.project_identifier}-api-gateway-logs"
  }
}


# Stage padrão com logs habilitados
resource "aws_apigatewayv2_stage" "default" {
  api_id      = aws_apigatewayv2_api.main.id
  name        = "$default"
  auto_deploy = true

  access_log_settings {
    destination_arn = aws_cloudwatch_log_group.api_gateway.arn
    format = jsonencode({
      requestId         = "$context.requestId"
      routeKey          = "$context.routeKey"
      httpMethod        = "$context.httpMethod"
      status            = "$context.status"
      integrationStatus = "$context.integrationStatus"
      errorMessage      = "$context.error.message"
      errorType         = "$context.error.responseType"
      authorizerSub     = "$context.authorizer.sub"
      authorizerRole    = "$context.authorizer.role"
    })
  }


  default_route_settings {
    detailed_metrics_enabled = true
    logging_level            = "INFO"
    throttling_burst_limit   = 5000
    throttling_rate_limit    = 10000
  }

  tags = {
    Name = "${var.project_identifier}-api-gateway-stage"
  }
}

# Permissão para API Gateway invocar a Lambda de Login
resource "aws_lambda_permission" "api_gateway_login" {
  statement_id  = "AllowAPIGatewayInvokeLogin"
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.login.function_name
  principal     = "apigateway.amazonaws.com"
  source_arn    = "${aws_apigatewayv2_api.main.execution_arn}/*/*"
}

# Permissão para API Gateway invocar a Lambda Authorizer
resource "aws_lambda_permission" "api_gateway_authorizer" {
  statement_id  = "AllowAPIGatewayInvokeAuthorizer"
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.authorizer.function_name
  principal     = "apigateway.amazonaws.com"
  source_arn    = "${aws_apigatewayv2_api.main.execution_arn}/authorizers/*"
}

# Integração com Lambda de Login
resource "aws_apigatewayv2_integration" "lambda_login" {
  api_id                 = aws_apigatewayv2_api.main.id
  integration_type       = "AWS_PROXY"
  integration_uri        = aws_lambda_function.login.arn
  integration_method     = "POST"
  payload_format_version = "2.0"
}

# Lambda Authorizer customizado para validar tokens JWT (HTTP API v2)
resource "aws_apigatewayv2_authorizer" "lambda_auth" {
  api_id                            = aws_apigatewayv2_api.main.id
  authorizer_type                   = "REQUEST"
  authorizer_uri                    = aws_lambda_function.authorizer.invoke_arn
  name                              = "${var.project_identifier}-lambda-authorizer"
  authorizer_payload_format_version = "2.0"
  authorizer_result_ttl_in_seconds  = 300
  identity_sources                  = ["$request.header.Authorization"]
  enable_simple_responses           = true
}

# Security Group para VPC Link
resource "aws_security_group" "vpc_link_sg" {
  name        = "${var.project_identifier}-vpc-link-sg"
  description = "Security group para VPC Link do API Gateway"
  vpc_id      = data.terraform_remote_state.infra.outputs.vpc_principal_id

  egress {
    description = "Permitir todo trafego de saida"
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "${var.project_identifier}-vpc-link-sg"
  }
}

# VPC Link para conectar API Gateway ao NLB privado
resource "aws_apigatewayv2_vpc_link" "eks" {
  name               = "${var.project_identifier}-vpc-link"
  security_group_ids = [aws_security_group.vpc_link_sg.id]
  subnet_ids         = data.terraform_remote_state.infra.outputs.subnet_publica_ids

  tags = {
    Name = "${var.project_identifier}-vpc-link"
  }
}

# Integração com EKS via VPC Link/NLB (service-order — listener porta 80)
resource "aws_apigatewayv2_integration" "eks" {
  api_id                 = aws_apigatewayv2_api.main.id
  integration_type       = "HTTP_PROXY"
  integration_uri        = data.terraform_remote_state.infra.outputs.nlb_listener_arn
  integration_method     = "ANY"
  connection_type        = "VPC_LINK"
  connection_id          = aws_apigatewayv2_vpc_link.eks.id
  payload_format_version = "1.0"
  request_parameters = {
    "overwrite:path"                        = "/$request.path.proxy"
    "overwrite:header.x-authenticated-sub"  = "$context.authorizer.sub"
    "overwrite:header.x-authenticated-role" = "$context.authorizer.role"
  }
}

# Integrações por MS — com path stripping (overwrite:path remove o prefixo /ms-name/)
# Usado pelas rotas protegidas de API: GET /customer/api/customers → GET /api/customers
resource "aws_apigatewayv2_integration" "ms" {
  for_each               = data.terraform_remote_state.infra.outputs.nlb_ms_listener_arns
  api_id                 = aws_apigatewayv2_api.main.id
  integration_type       = "HTTP_PROXY"
  integration_uri        = each.value
  integration_method     = "ANY"
  connection_type        = "VPC_LINK"
  connection_id          = aws_apigatewayv2_vpc_link.eks.id
  payload_format_version = "1.0"
  request_parameters = {
    "overwrite:path"                        = "/$request.path.proxy"
    "overwrite:header.x-authenticated-sub"  = "$context.authorizer.sub"
    "overwrite:header.x-authenticated-role" = "$context.authorizer.role"
  }
}

# Integração dedicada para proxy de auth via customer MS
# Rota publica: POST /customer/auth/login -> customer pod -> proxy para Lambda auth
# overwrite:path estatico: /customer/auth/login -> /auth/login no pod
resource "aws_apigatewayv2_integration" "customer_auth_proxy" {
  api_id                 = aws_apigatewayv2_api.main.id
  integration_type       = "HTTP_PROXY"
  integration_uri        = data.terraform_remote_state.infra.outputs.nlb_ms_listener_arns["customer"]
  integration_method     = "ANY"
  connection_type        = "VPC_LINK"
  connection_id          = aws_apigatewayv2_vpc_link.eks.id
  payload_format_version = "1.0"
  request_parameters = {
    "overwrite:path" = "/auth/login"
  }
}
# Usado pelas rotas publicas de Swagger: GET /customer/swagger-ui/index.html
# passa para o backend como /customer/swagger-ui/index.html (springdoc configurado com prefixo)
resource "aws_apigatewayv2_integration" "ms_swagger" {
  for_each               = data.terraform_remote_state.infra.outputs.nlb_ms_listener_arns
  api_id                 = aws_apigatewayv2_api.main.id
  integration_type       = "HTTP_PROXY"
  integration_uri        = each.value
  integration_method     = "ANY"
  connection_type        = "VPC_LINK"
  connection_id          = aws_apigatewayv2_vpc_link.eks.id
  payload_format_version = "1.0"
  # Sem overwrite:path — springdoc em cada MS serve em /<ms-name>/* paths
}