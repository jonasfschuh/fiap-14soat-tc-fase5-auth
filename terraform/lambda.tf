# =============================================================================
# lambda.tf - Fase 5
# A Lambda de autenticacao acessa diretamente o banco customer_db no RDS compartilhado
# provisionado em fiap-14soat-tc-fase5-iac-terraform.
#
# PRE-REQUISITO: Deploy do iac-terraform (infra) deve ter sido executado antes,
# pois o tfstate do RDS compartilhado precisa existir em:
#   s3://<bucket>/infra/terraform.tfstate
#
# Outputs esperados do infra tfstate:
#   rds_host, rds_port, rds_master_username
# =============================================================================

# Data source - RDS compartilhado provisionado pelo iac-terraform
data "terraform_remote_state" "banco" {
  backend = "s3"
  config = {
    bucket = var.infra_terraform_state_bucket
    key    = "infra/terraform.tfstate"
    region = var.aws_region
  }
}

# Security Group para Lambda acessar RDS
resource "aws_security_group" "lambda_sg" {
  count       = var.lambda_enable_vpc ? 1 : 0
  name        = "${var.project_identifier}-lambda-sg"
  description = "Security group para Lambda acessar RDS do MS Customer"
  vpc_id      = data.terraform_remote_state.infra.outputs.vpc_principal_id

  egress {
    description = "Acesso PostgreSQL"
    from_port   = 5432
    to_port     = 5432
    protocol    = "tcp"
    cidr_blocks = [data.terraform_remote_state.infra.outputs.vpc_principal_cidr]
  }

  egress {
    description = "HTTPS para internet"
    from_port   = 443
    to_port     = 443
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    description = "HTTP para internet"
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "${var.project_identifier}-lambda-sg"
  }
}

# Fonte de dados para criar arquivo zip do codigo da Lambda
data "archive_file" "lambda_zip" {
  type        = "zip"
  source_dir  = "${path.module}/../src/auth-lambda/target/lambda-package"
  output_path = "${path.module}/lambda_function.zip"
}

locals {
  # RDS compartilhado - host ja vem limpo (sem porta) do output rds_host
  postgres_host    = data.terraform_remote_state.banco.outputs.rds_host
  postgres_port    = data.terraform_remote_state.banco.outputs.rds_port
  postgres_user    = data.terraform_remote_state.banco.outputs.rds_master_username
  customer_db_name = "customer_db"
}

# Lambda Function para Login (geracao de tokens)
resource "aws_lambda_function" "login" {
  filename         = data.archive_file.lambda_zip.output_path
  function_name    = "${var.project_identifier}-login-lambda"
  role             = local.lambda_execution_role_arn
  handler          = "br.com.fiap.authlambda.handler.LoginHandler::handleRequest"
  source_code_hash = data.archive_file.lambda_zip.output_base64sha256
  runtime          = var.lambda_runtime
  timeout          = var.lambda_timeout
  memory_size      = var.lambda_memory_size

  layers = [
    "arn:aws:lambda:us-east-1:451483290750:layer:NewRelicLambdaExtension:37"
  ]

  dynamic "vpc_config" {
    for_each = var.lambda_enable_vpc ? [1] : []
    content {
      subnet_ids         = data.terraform_remote_state.infra.outputs.subnet_publica_ids
      security_group_ids = [aws_security_group.lambda_sg[0].id]
    }
  }

  environment {
    variables = {
      JWT_SECRET        = var.jwt_key
      JWT_ISSUER        = var.jwt_issuer
      JWT_EXPIRATION_MS = tostring(var.jwt_expiration_ms)
      DB_URL            = "jdbc:postgresql://${local.postgres_host}:${local.postgres_port}/${local.customer_db_name}?sslmode=require"
      DB_USER           = local.postgres_user
      DB_PASSWORD       = var.db_password
      DB_POOL_SIZE      = "2"

      NEW_RELIC_ACCOUNT_ID                    = var.new_relic_account_id
      NEW_RELIC_LICENSE_KEY                   = var.new_relic_license_key
      NEW_RELIC_EXTENSION_SEND_FUNCTION_LOGS  = "true"
      NEW_RELIC_EXTENSION_SEND_EXTENSION_LOGS = "false"
      NEW_RELIC_APP_NAME                      = "${var.project_identifier}-login-lambda"
    }
  }

  tags = {
    Name = "${var.project_identifier}-login-lambda"
  }
}

# Lambda Function para Authorizer (validacao de tokens)
resource "aws_lambda_function" "authorizer" {
  filename         = data.archive_file.lambda_zip.output_path
  function_name    = "${var.project_identifier}-authorizer-lambda"
  role             = local.lambda_execution_role_arn
  handler          = "br.com.fiap.authlambda.handler.JwtAuthorizerHandler::handleRequest"
  source_code_hash = data.archive_file.lambda_zip.output_base64sha256
  runtime          = var.lambda_runtime
  timeout          = var.lambda_timeout
  memory_size      = var.lambda_memory_size

  layers = [
    "arn:aws:lambda:us-east-1:451483290750:layer:NewRelicLambdaExtension:37"
  ]

  environment {
    variables = {
      JWT_SECRET        = var.jwt_key
      JWT_ISSUER        = var.jwt_issuer
      JWT_EXPIRATION_MS = tostring(var.jwt_expiration_ms)
      DB_URL            = "jdbc:postgresql://${local.postgres_host}:${local.postgres_port}/${local.customer_db_name}?sslmode=require"
      DB_USER           = local.postgres_user
      DB_PASSWORD       = var.db_password
      DB_POOL_SIZE      = "2"

      NEW_RELIC_ACCOUNT_ID                    = var.new_relic_account_id
      NEW_RELIC_LICENSE_KEY                   = var.new_relic_license_key
      NEW_RELIC_EXTENSION_SEND_FUNCTION_LOGS  = "true"
      NEW_RELIC_EXTENSION_SEND_EXTENSION_LOGS = "false"
      NEW_RELIC_APP_NAME                      = "${var.project_identifier}-authorizer-lambda"
    }
  }

  tags = {
    Name = "${var.project_identifier}-authorizer-lambda"
  }
}