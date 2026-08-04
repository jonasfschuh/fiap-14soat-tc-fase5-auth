terraform {
  backend "s3" {
    bucket  = "fiap-14soat-fase5-jonasfschuh"
    key     = "lambda-auth/terraform.tfstate"
    region  = "us-east-1"
    encrypt = true
  }
}
