variable "aws_region" {
  type    = string
  default = "us-east-1"
}

variable "availability_zones" {
  type        = list(string)
  description = "Two stable Availability Zone names for the production VPC; update when selecting another AWS region."
  default     = ["us-east-1a", "us-east-1b"]
  validation {
    condition     = length(var.availability_zones) == 2
    error_message = "Exactly two Availability Zones are required."
  }
}

variable "project_name" {
  type    = string
  default = "fiapx-video-platform"
}

variable "terraform_state_bucket_name" {
  type    = string
  default = "fiapx-video-platform-terraform-state"
}

variable "root_domain" {
  type        = string
  description = "Existing Route 53 hosted zone, without a trailing dot."
}

variable "alert_email" {
  type        = string
  description = "CloudWatch alarm notification recipient."
}

variable "jwt_private_key_base64" {
  type      = string
  sensitive = true
}
variable "jwt_public_key_base64" {
  type      = string
  sensitive = true
}
variable "ses_smtp_username" {
  type      = string
  sensitive = true
}
variable "ses_smtp_password" {
  type      = string
  sensitive = true
}
variable "default_notification_recipient" { type = string }
variable "api_image" {
  type        = string
  description = "Immutable image reference including digest."
}
variable "processor_image" {
  type        = string
  description = "Immutable image reference including digest."
}
variable "notification_image" {
  type        = string
  description = "Immutable image reference including digest."
}
variable "collector_image" {
  type        = string
  description = "Immutable ADOT collector image reference including digest."
}
variable "nat_gateway_enabled" {
  type    = bool
  default = true
}

variable "db_instance_class" {
  type    = string
  default = "db.t4g.micro"
}

variable "api_desired_count" {
  type    = number
  default = 0
}

variable "processor_desired_count" {
  type    = number
  default = 0
}

variable "notification_desired_count" {
  type    = number
  default = 0
}
