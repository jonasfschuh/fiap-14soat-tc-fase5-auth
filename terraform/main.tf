provider "aws" {
  region = var.aws_region

  default_tags {
    tags = {
      Project   = "fiap-14soat-fase5-raceforce"
      ManagedBy = "Terraform"
      Component = "Lambda-Auth"
    }
  }
}

data "aws_caller_identity" "current" {}

locals {
  lambda_execution_role_arn = trimspace(var.lambda_execution_role_arn) != "" ? trimspace(var.lambda_execution_role_arn) : "arn:aws:iam::${data.aws_caller_identity.current.account_id}:role/LabRole"
}

