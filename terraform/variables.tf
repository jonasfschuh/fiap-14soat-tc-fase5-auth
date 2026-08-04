variable "aws_region" {
  description = "AWS region onde os recursos serao criados"
  type        = string
  default     = "us-east-1"
}

variable "project_identifier" {
  description = "Identificador do projeto para nomeacao de recursos"
  type        = string
  default     = "fiap-14soat-fase5-raceforce"
}

variable "lambda_execution_role_arn" {
  description = "ARN da role de execucao da Lambda. Se vazio, usa a LabRole da conta atual."
  type        = string
  default     = ""
}

variable "lambda_runtime" {
  description = "Runtime da funcao Lambda"
  type        = string
  default     = "java21"
}

variable "jwt_expiration_ms" {
  description = "Tempo de expiracao do JWT em milissegundos"
  type        = number
  default     = 86400000
}

variable "lambda_timeout" {
  description = "Timeout da funcao Lambda em segundos"
  type        = number
  default     = 30
}

variable "lambda_memory_size" {
  description = "Memoria alocada para a funcao Lambda em MB"
  type        = number
  default     = 512
}

variable "lambda_enable_vpc" {
  description = "Controla se as Lambdas de auth executam dentro da VPC. Desabilite quando precisar garantir egress para New Relic sem NAT."
  type        = bool
  default     = true
}

variable "jwt_key" {
  description = "Chave secreta para geracao de tokens JWT"
  type        = string
  sensitive   = true
}

variable "jwt_issuer" {
  description = "Issuer do token JWT"
  type        = string
  default     = "RaceForceAuthService"
}

variable "enable_lambda_authorizer" {
  description = "Controla se as rotas proxy do API Gateway usam Lambda Authorizer"
  type        = bool
  default     = true
}

# Remote state — infraestrutura (EKS/NLB)
variable "infra_terraform_state_bucket" {
  description = "Nome do bucket S3 onde esta o state da infraestrutura"
  type        = string
  default     = "fiap-14soat-fase5-jonasfschuh"
}

# Remote state — banco do MS Customer (Fase 5)
# O banco do customer eh a fonte de usuarios para autenticacao JWT
variable "banco_terraform_state_bucket" {
  description = "Nome do bucket S3 onde esta o state do banco do MS Customer"
  type        = string
  default     = "fiap-14soat-fase5-jonasfschuh"
}

variable "customer_db_state_key" {
  description = "Chave do tfstate do banco do MS Customer no bucket S3"
  type        = string
  default     = "customer/database/terraform.tfstate"
}

# Senha do banco (passada via workflow)
variable "db_password" {
  description = "Senha do banco de dados do MS Customer"
  type        = string
  sensitive   = true
}

variable "new_relic_account_id" {
  description = "ID da conta do New Relic"
  type        = string
  sensitive   = true
}

variable "new_relic_license_key" {
  description = "Chave de licenca do New Relic (Ingest License)"
  type        = string
  sensitive   = true
}
