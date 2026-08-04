# Outputs das funcoes Lambda
output "login_lambda_arn" {
  description = "ARN da funcao Lambda de Login"
  value       = aws_lambda_function.login.arn
}

output "login_lambda_name" {
  description = "Nome da funcao Lambda de Login"
  value       = aws_lambda_function.login.function_name
}

output "authorizer_lambda_arn" {
  description = "ARN da funcao Lambda Authorizer"
  value       = aws_lambda_function.authorizer.arn
}

output "authorizer_lambda_name" {
  description = "Nome da funcao Lambda Authorizer"
  value       = aws_lambda_function.authorizer.function_name
}

output "lambda_execution_role_arn" {
  description = "ARN da role de execucao da Lambda"
  value       = local.lambda_execution_role_arn
}

# Outputs do API Gateway
output "api_gateway_id" {
  description = "ID do API Gateway"
  value       = aws_apigatewayv2_api.main.id
}

output "api_gateway_endpoint" {
  description = "Endpoint base do API Gateway"
  value       = aws_apigatewayv2_api.main.api_endpoint
}

# Alias usado pelo deploy.yaml do service-order para injetar AUTH_LAMBDA_URL no Swagger
output "api_gateway_url" {
  description = "URL publica do API Gateway — usar como AUTH_LAMBDA_URL no service-order"
  value       = aws_apigatewayv2_api.main.api_endpoint
}

output "api_gateway_login_url" {
  description = "URL completa do endpoint de login — POST <url>/auth/login"
  value       = "${aws_apigatewayv2_api.main.api_endpoint}/auth/login"
}

output "api_gateway_official_endpoint" {
  description = "Endpoint oficial para consumidores externos"
  value       = aws_apigatewayv2_api.main.api_endpoint
}

output "api_gateway_custom_domain_url" {
  description = "URL de dominio customizado (desativado nesta fase)"
  value       = null
}

output "api_gateway_execution_arn" {
  description = "ARN de execucao do API Gateway"
  value       = aws_apigatewayv2_api.main.execution_arn
}

output "vpc_link_id" {
  description = "ID do VPC Link"
  value       = aws_apigatewayv2_vpc_link.eks.id
}

# Outputs do CloudWatch Logs
output "api_gateway_log_group_name" {
  description = "Nome do CloudWatch Log Group do API Gateway"
  value       = aws_cloudwatch_log_group.api_gateway.name
}

output "api_gateway_log_group_arn" {
  description = "ARN do CloudWatch Log Group do API Gateway"
  value       = aws_cloudwatch_log_group.api_gateway.arn
}
