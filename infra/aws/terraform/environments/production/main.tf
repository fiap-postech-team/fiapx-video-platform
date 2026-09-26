provider "aws" {
  region = var.aws_region
  default_tags {
    tags = {
      Project     = var.project_name
      Environment = "production"
      ManagedBy   = "Terraform"
      Repository  = "fiapx-video-platform"
    }
  }
}

provider "random" {}

data "aws_caller_identity" "current" {}
data "aws_route53_zone" "root" { name = "${var.root_domain}." }

locals {
  azs              = var.availability_zones
  service_names    = ["video-api", "video-processor", "notification-worker"]
  repository_names = concat(local.service_names, ["adot-collector"])
  service_ports    = { video-api = 8080, video-processor = 8081, notification-worker = 8082 }
  service_images = {
    video-api           = var.api_image
    video-processor     = var.processor_image
    notification-worker = var.notification_image
  }
  name_prefix = "${var.project_name}-prod"
  mq_host     = trimprefix(split(":", trimprefix(aws_mq_broker.rabbitmq.instances[0].endpoints[0], "amqps://"))[0], "//")
}

resource "aws_vpc" "main" {
  cidr_block           = "10.20.0.0/16"
  enable_dns_hostnames = true
  enable_dns_support   = true
  tags                 = { Name = "${local.name_prefix}-vpc" }
  #checkov:skip=CKV2_AWS_11:VPC flow logs incur continuous CloudWatch ingestion and storage charges; application and service metrics/logs are enabled, with flow logs deferred until traffic and retention needs are measured.
}

resource "aws_internet_gateway" "main" { vpc_id = aws_vpc.main.id }

resource "aws_subnet" "public" {
  count                   = 2
  vpc_id                  = aws_vpc.main.id
  cidr_block              = ["10.20.0.0/20", "10.20.16.0/20"][count.index]
  availability_zone       = local.azs[count.index]
  map_public_ip_on_launch = false
  tags                    = { Name = "${local.name_prefix}-public-${count.index + 1}" }
}

resource "aws_subnet" "application" {
  count             = 2
  vpc_id            = aws_vpc.main.id
  cidr_block        = ["10.20.32.0/20", "10.20.48.0/20"][count.index]
  availability_zone = local.azs[count.index]
  tags              = { Name = "${local.name_prefix}-application-${count.index + 1}" }
}

resource "aws_subnet" "data" {
  count             = 2
  vpc_id            = aws_vpc.main.id
  cidr_block        = ["10.20.64.0/20", "10.20.80.0/20"][count.index]
  availability_zone = local.azs[count.index]
  tags              = { Name = "${local.name_prefix}-data-${count.index + 1}" }
}

resource "aws_route_table" "public" {
  vpc_id = aws_vpc.main.id
  route {
    cidr_block = "0.0.0.0/0"
    gateway_id = aws_internet_gateway.main.id
  }
}
resource "aws_route_table_association" "public" {
  count          = 2
  subnet_id      = aws_subnet.public[count.index].id
  route_table_id = aws_route_table.public.id
}

resource "aws_eip" "nat" {
  count  = var.nat_gateway_enabled ? 1 : 0
  domain = "vpc"
  #checkov:skip=CKV2_AWS_19:This Elastic IP is attached to the NAT Gateway below; Checkov does not model the aws_nat_gateway allocation_id reference.
}

resource "aws_default_security_group" "main" {
  vpc_id  = aws_vpc.main.id
  ingress = []
  egress  = []
}
resource "aws_nat_gateway" "main" {
  count         = var.nat_gateway_enabled ? 1 : 0
  allocation_id = aws_eip.nat[0].id
  subnet_id     = aws_subnet.public[0].id
  depends_on    = [aws_internet_gateway.main]
}
resource "aws_route_table" "application" {
  count  = 2
  vpc_id = aws_vpc.main.id
  dynamic "route" {
    for_each = var.nat_gateway_enabled ? [1] : []
    content {
      cidr_block     = "0.0.0.0/0"
      nat_gateway_id = aws_nat_gateway.main[0].id
    }
  }
}
resource "aws_route_table_association" "application" {
  count          = 2
  subnet_id      = aws_subnet.application[count.index].id
  route_table_id = aws_route_table.application[count.index].id
}
resource "aws_route_table" "data" {
  count  = 2
  vpc_id = aws_vpc.main.id
}
resource "aws_route_table_association" "data" {
  count          = 2
  subnet_id      = aws_subnet.data[count.index].id
  route_table_id = aws_route_table.data[count.index].id
}

resource "aws_vpc_endpoint" "s3" {
  vpc_id            = aws_vpc.main.id
  service_name      = "com.amazonaws.${var.aws_region}.s3"
  vpc_endpoint_type = "Gateway"
  route_table_ids   = concat(aws_route_table.application[*].id, aws_route_table.data[*].id)
}

resource "aws_security_group" "alb" {
  name        = "${local.name_prefix}-alb"
  description = "Accept HTTPS from CloudFront and forward API requests to private VPC targets."
  vpc_id      = aws_vpc.main.id
  ingress {
    description     = "HTTPS origin requests from CloudFront origin-facing addresses."
    from_port       = 443
    to_port         = 443
    protocol        = "tcp"
    prefix_list_ids = [data.aws_ec2_managed_prefix_list.cloudfront.id]
  }
  egress {
    description = "Forward API traffic to targets inside the VPC."
    from_port   = 8080
    to_port     = 8080
    protocol    = "tcp"
    cidr_blocks = [aws_vpc.main.cidr_block]
  }
}
data "aws_ec2_managed_prefix_list" "cloudfront" { name = "com.amazonaws.global.cloudfront.origin-facing" }

resource "aws_security_group" "ecs" {
  name        = "${local.name_prefix}-ecs"
  description = "Private ECS task ingress and application egress."
  vpc_id      = aws_vpc.main.id
  ingress {
    description = "Allow communication between trusted tasks in this security group."
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    self        = true
  }
  egress {
    description = "HTTPS to AWS APIs and external dependencies through the private subnet NAT route."
    from_port   = 443
    to_port     = 443
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }
  egress {
    description = "STARTTLS SMTP delivery to Amazon SES."
    from_port   = 587
    to_port     = 587
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }
  egress {
    description = "PostgreSQL and AMQPS traffic to private data services in the VPC."
    from_port   = 5432
    to_port     = 5432
    protocol    = "tcp"
    cidr_blocks = [aws_vpc.main.cidr_block]
  }
  egress {
    description = "AMQPS traffic to the private RabbitMQ service in the VPC."
    from_port   = 5671
    to_port     = 5671
    protocol    = "tcp"
    cidr_blocks = [aws_vpc.main.cidr_block]
  }
}
resource "aws_security_group" "api" {
  name        = "${local.name_prefix}-api"
  description = "Video API tasks reachable only from the ALB."
  vpc_id      = aws_vpc.main.id
  ingress {
    description     = "HTTP from the ALB to the video API target port."
    from_port       = 8080
    to_port         = 8080
    protocol        = "tcp"
    security_groups = [aws_security_group.alb.id]
  }
  egress {
    description = "HTTPS to required external services through the private subnet NAT route."
    from_port   = 443
    to_port     = 443
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }
  egress {
    description = "PostgreSQL traffic to the private RDS instance in the VPC."
    from_port   = 5432
    to_port     = 5432
    protocol    = "tcp"
    cidr_blocks = [aws_vpc.main.cidr_block]
  }
  egress {
    description = "AMQPS traffic to the private RabbitMQ broker in the VPC."
    from_port   = 5671
    to_port     = 5671
    protocol    = "tcp"
    cidr_blocks = [aws_vpc.main.cidr_block]
  }
}
resource "aws_security_group" "rds" {
  name        = "${local.name_prefix}-rds"
  description = "PostgreSQL ingress from private ECS tasks only."
  vpc_id      = aws_vpc.main.id
  ingress {
    description     = "PostgreSQL from the private ECS task security group."
    from_port       = 5432
    to_port         = 5432
    protocol        = "tcp"
    security_groups = [aws_security_group.ecs.id]
  }
}
resource "aws_security_group" "mq" {
  name        = "${local.name_prefix}-mq"
  description = "TLS RabbitMQ ingress from private ECS tasks only."
  vpc_id      = aws_vpc.main.id
  ingress {
    description     = "AMQPS from the private ECS task security group."
    from_port       = 5671
    to_port         = 5671
    protocol        = "tcp"
    security_groups = [aws_security_group.ecs.id]
  }
}

resource "aws_cloudwatch_log_group" "services" {
  for_each          = toset(local.service_names)
  name              = "/ecs/${local.name_prefix}/${each.key}"
  retention_in_days = 30
  #checkov:skip=CKV_AWS_158:CloudWatch Logs uses the AWS-owned service encryption key by default; a customer-managed key adds per-ingest and per-read KMS charges.
  #checkov:skip=CKV_AWS_338:Thirty-day retention controls CloudWatch storage cost for the initial environment; extend after measuring volume and compliance requirements.
}
resource "aws_cloudwatch_log_group" "adot" {
  name              = "/ecs/${local.name_prefix}/adot"
  retention_in_days = 30
  #checkov:skip=CKV_AWS_158:CloudWatch Logs uses the AWS-owned service encryption key by default; a customer-managed key adds per-ingest and per-read KMS charges.
  #checkov:skip=CKV_AWS_338:Thirty-day retention controls CloudWatch storage cost for the initial environment; extend after measuring volume and compliance requirements.
}

resource "aws_ecr_repository" "services" {
  for_each             = toset(local.repository_names)
  name                 = "${var.project_name}/${each.key}"
  image_tag_mutability = "IMMUTABLE"
  image_scanning_configuration { scan_on_push = true }
  encryption_configuration { encryption_type = "AES256" }
  #checkov:skip=CKV_AWS_136:ECR uses AWS-managed AES-256 encryption at rest; a customer-managed key adds recurring KMS key and request costs without changing the repository access boundary.
}
resource "aws_ecr_lifecycle_policy" "services" {
  for_each   = aws_ecr_repository.services
  repository = each.value.name
  policy     = jsonencode({ rules = [{ rulePriority = 1, description = "Retain 30 recent images", selection = { tagStatus = "any", countType = "imageCountMoreThan", countNumber = 30 }, action = { type = "expire" } }] })
}

resource "aws_s3_bucket" "media" {
  bucket_prefix = "${var.project_name}-media-"
  #checkov:skip=CKV_AWS_145:SSE-S3 AES-256 encrypts video objects at rest without per-request KMS charges on high-volume media traffic.
  #checkov:skip=CKV_AWS_18:Dedicated access-log buckets add storage and request cost; defer until the organization defines an access-audit retention requirement.
  #checkov:skip=CKV_AWS_144:Cross-region replication would duplicate video storage cost; defer until recovery objectives are defined.
  #checkov:skip=CKV2_AWS_62:Uploads use presigned URLs and the application publishes processing jobs through RabbitMQ; S3 events are not part of this event flow.
}
resource "aws_s3_bucket_public_access_block" "media" {
  bucket                  = aws_s3_bucket.media.id
  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}
resource "aws_s3_bucket_server_side_encryption_configuration" "media" {
  bucket = aws_s3_bucket.media.id
  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"
    }
  }
}
resource "aws_s3_bucket_versioning" "media" {
  bucket = aws_s3_bucket.media.id
  versioning_configuration { status = "Enabled" }
}
resource "aws_s3_bucket_cors_configuration" "media" {
  bucket = aws_s3_bucket.media.id
  cors_rule {
    allowed_headers = ["*"]
    allowed_methods = ["PUT", "GET", "HEAD"]
    allowed_origins = ["https://app.${var.root_domain}"]
    expose_headers  = ["ETag"]
    max_age_seconds = 3000
  }
}
resource "aws_s3_bucket_lifecycle_configuration" "media" {
  bucket = aws_s3_bucket.media.id
  rule {
    id     = "abort-incomplete-uploads"
    status = "Enabled"
    filter {}
    abort_incomplete_multipart_upload { days_after_initiation = 7 }
  }
}

resource "aws_s3_bucket" "frontend" {
  bucket_prefix = "${var.project_name}-frontend-"
  #checkov:skip=CKV_AWS_145:SSE-S3 AES-256 encrypts frontend objects at rest without KMS request charges for routine CDN reads and deployments.
  #checkov:skip=CKV_AWS_18:Dedicated access-log buckets add storage and request cost; defer until access-audit retention requirements are defined.
  #checkov:skip=CKV_AWS_144:Cross-region replication is deferred in the cost-controlled initial environment.
  #checkov:skip=CKV2_AWS_62:Frontend releases are uploaded by the deployment workflow and served by CloudFront; there is no S3 event consumer.
}
resource "aws_s3_bucket_public_access_block" "frontend" {
  bucket                  = aws_s3_bucket.frontend.id
  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}
resource "aws_s3_bucket_server_side_encryption_configuration" "frontend" {
  bucket = aws_s3_bucket.frontend.id
  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"
    }
  }
}
resource "aws_s3_bucket_versioning" "frontend" {
  bucket = aws_s3_bucket.frontend.id
  versioning_configuration { status = "Enabled" }
}
resource "aws_s3_bucket_lifecycle_configuration" "frontend" {
  bucket = aws_s3_bucket.frontend.id

  rule {
    id     = "expire-old-frontend-versions"
    status = "Enabled"
    filter {}
    noncurrent_version_expiration { noncurrent_days = 30 }
    abort_incomplete_multipart_upload { days_after_initiation = 7 }
  }
}
resource "aws_cloudfront_origin_access_control" "frontend" {
  name                              = local.name_prefix
  origin_access_control_origin_type = "s3"
  signing_behavior                  = "always"
  signing_protocol                  = "sigv4"
}
resource "aws_cloudfront_function" "spa" {
  name    = "${local.name_prefix}-spa-fallback"
  runtime = "cloudfront-js-2.0"
  publish = true
  code    = file("${path.module}/../../observability/spa-fallback.js")
}

resource "aws_acm_certificate" "edge" {
  domain_name               = "app.${var.root_domain}"
  subject_alternative_names = ["origin.${var.root_domain}"]
  validation_method         = "DNS"
  lifecycle { create_before_destroy = true }
}
resource "aws_route53_record" "cert_validation" {
  for_each = { for dvo in aws_acm_certificate.edge.domain_validation_options : dvo.domain_name => dvo }
  zone_id  = data.aws_route53_zone.root.zone_id
  name     = each.value.resource_record_name
  type     = each.value.resource_record_type
  records  = [each.value.resource_record_value]
  ttl      = 60
}
resource "aws_acm_certificate_validation" "edge" {
  certificate_arn         = aws_acm_certificate.edge.arn
  validation_record_fqdns = [for record in aws_route53_record.cert_validation : record.fqdn]
}

resource "random_password" "origin_header" {
  length  = 48
  special = false
}
resource "aws_lb" "api" {
  name                       = "${local.name_prefix}-api"
  internal                   = false
  load_balancer_type         = "application"
  security_groups            = [aws_security_group.alb.id]
  subnets                    = aws_subnet.public[*].id
  enable_deletion_protection = true
  drop_invalid_header_fields = true
  desync_mitigation_mode     = "strictest"
  #checkov:skip=CKV_AWS_91:ALB access logging requires a dedicated S3 destination bucket and ongoing log storage/requests; CloudWatch ALB metrics and alarms remain enabled.
  #checkov:skip=CKV2_AWS_28:CloudFront-only origin ingress and a secret origin-verification header protect this origin; an additional WAF is deferred to control monthly cost.
}
resource "aws_lb_target_group" "api" {
  name        = "${local.name_prefix}-api"
  port        = 8080
  protocol    = "HTTP"
  target_type = "ip"
  vpc_id      = aws_vpc.main.id
  #checkov:skip=CKV_AWS_378:TLS terminates at the HTTPS ALB; target traffic stays in private subnets and ingress is restricted to the ALB security group.
  health_check {
    path     = "/actuator/health"
    matcher  = "200-399"
    interval = 30
  }
}
resource "aws_lb_listener" "api" {
  load_balancer_arn = aws_lb.api.arn
  port              = 443
  protocol          = "HTTPS"
  certificate_arn   = aws_acm_certificate_validation.edge.certificate_arn
  ssl_policy        = "ELBSecurityPolicy-TLS13-1-2-2021-06"
  default_action {
    type = "fixed-response"
    fixed_response {
      content_type = "text/plain"
      message_body = "Forbidden"
      status_code  = "403"
    }
  }
}
resource "aws_lb_listener_rule" "cloudfront_only" {
  listener_arn = aws_lb_listener.api.arn
  priority     = 10
  condition {
    http_header {
      http_header_name = "X-Origin-Verify"
      values           = [random_password.origin_header.result]
    }
  }
  action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.api.arn
  }
}

resource "aws_cloudfront_distribution" "app" {
  enabled             = true
  default_root_object = "index.html"
  aliases             = ["app.${var.root_domain}"]
  price_class         = "PriceClass_100"
  #checkov:skip=CKV_AWS_68:AWS WAF for CloudFront adds recurring charges; the origin is restricted to CloudFront and requires a high-entropy verification header.
  #checkov:skip=CKV2_AWS_47:WAF is intentionally deferred in this cost-controlled baseline; if enabled later, include current Log4j managed protections.
  #checkov:skip=CKV_AWS_86:CloudFront access logging requires a dedicated S3 bucket and recurring storage/requests; CloudWatch and AMP metrics remain enabled.
  #checkov:skip=CKV_AWS_310:SPA and API use distinct origins selected by path behavior; failing over between them would return incompatible content.
  #checkov:skip=CKV_AWS_374:The app is intended to be globally available; geography is not an access-control requirement.
  origin {
    domain_name              = aws_s3_bucket.frontend.bucket_regional_domain_name
    origin_id                = "frontend-s3"
    origin_access_control_id = aws_cloudfront_origin_access_control.frontend.id
  }
  origin {
    domain_name = "origin.${var.root_domain}"
    origin_id   = "api-alb"
    custom_origin_config {
      http_port              = 80
      https_port             = 443
      origin_protocol_policy = "https-only"
      origin_ssl_protocols   = ["TLSv1.2"]
    }
    custom_header {
      name  = "X-Origin-Verify"
      value = random_password.origin_header.result
    }
  }
  default_cache_behavior {
    target_origin_id           = "frontend-s3"
    viewer_protocol_policy     = "redirect-to-https"
    allowed_methods            = ["GET", "HEAD", "OPTIONS"]
    cached_methods             = ["GET", "HEAD"]
    compress                   = true
    cache_policy_id            = data.aws_cloudfront_cache_policy.caching_optimized.id
    response_headers_policy_id = aws_cloudfront_response_headers_policy.security.id
    function_association {
      event_type   = "viewer-request"
      function_arn = aws_cloudfront_function.spa.arn
    }
  }
  ordered_cache_behavior {
    path_pattern               = "/v1/*"
    target_origin_id           = "api-alb"
    viewer_protocol_policy     = "redirect-to-https"
    allowed_methods            = ["DELETE", "GET", "HEAD", "OPTIONS", "PATCH", "POST", "PUT"]
    cached_methods             = ["GET", "HEAD"]
    cache_policy_id            = data.aws_cloudfront_cache_policy.caching_disabled.id
    origin_request_policy_id   = data.aws_cloudfront_origin_request_policy.all_viewer_except_host.id
    response_headers_policy_id = aws_cloudfront_response_headers_policy.security.id
  }
  restrictions {
    geo_restriction {
      restriction_type = "none"
    }
  }
  viewer_certificate {
    acm_certificate_arn      = aws_acm_certificate_validation.edge.certificate_arn
    ssl_support_method       = "sni-only"
    minimum_protocol_version = "TLSv1.2_2021"
  }
  depends_on = [aws_route53_record.origin]
}
resource "aws_cloudfront_response_headers_policy" "security" {
  name = "${local.name_prefix}-security-headers"
  security_headers_config {
    content_type_options { override = true }
    frame_options {
      frame_option = "DENY"
      override     = true
    }
    referrer_policy {
      referrer_policy = "strict-origin-when-cross-origin"
      override        = true
    }
    strict_transport_security {
      access_control_max_age_sec = 31536000
      include_subdomains         = true
      preload                    = true
      override                   = true
    }
  }
}
data "aws_cloudfront_cache_policy" "caching_optimized" { name = "Managed-CachingOptimized" }
data "aws_cloudfront_cache_policy" "caching_disabled" { name = "Managed-CachingDisabled" }
data "aws_cloudfront_origin_request_policy" "all_viewer_except_host" { name = "Managed-AllViewerExceptHostHeader" }
data "aws_iam_policy_document" "frontend_bucket" {
  statement {
    actions   = ["s3:GetObject"]
    resources = ["${aws_s3_bucket.frontend.arn}/*"]
    principals {
      type        = "Service"
      identifiers = ["cloudfront.amazonaws.com"]
    }
    condition {
      test     = "StringEquals"
      variable = "AWS:SourceArn"
      values   = [aws_cloudfront_distribution.app.arn]
    }
  }
}
resource "aws_s3_bucket_policy" "frontend" {
  bucket = aws_s3_bucket.frontend.id
  policy = data.aws_iam_policy_document.frontend_bucket.json
}
resource "aws_route53_record" "app" {
  zone_id = data.aws_route53_zone.root.zone_id
  name    = "app.${var.root_domain}"
  type    = "A"
  alias {
    name                   = aws_cloudfront_distribution.app.domain_name
    zone_id                = aws_cloudfront_distribution.app.hosted_zone_id
    evaluate_target_health = false
  }
}
resource "aws_route53_record" "origin" {
  zone_id = data.aws_route53_zone.root.zone_id
  name    = "origin.${var.root_domain}"
  type    = "A"
  alias {
    name                   = aws_lb.api.dns_name
    zone_id                = aws_lb.api.zone_id
    evaluate_target_health = true
  }
}

resource "aws_db_subnet_group" "main" {
  name       = local.name_prefix
  subnet_ids = aws_subnet.data[*].id
}
resource "random_password" "api_db" {
  length  = 40
  special = false
}
resource "random_password" "notification_db" {
  length  = 40
  special = false
}
resource "aws_db_instance" "main" {
  identifier                  = local.name_prefix
  engine                      = "postgres"
  instance_class              = var.db_instance_class
  allocated_storage           = 20
  max_allocated_storage       = 100
  storage_type                = "gp3"
  storage_encrypted           = true
  db_name                     = "fiapx"
  username                    = "dbadmin"
  manage_master_user_password = true
  db_subnet_group_name        = aws_db_subnet_group.main.name
  vpc_security_group_ids      = [aws_security_group.rds.id]
  publicly_accessible         = false
  multi_az                    = false
  #checkov:skip=CKV_AWS_157:Multi-AZ roughly doubles database compute cost; this initial environment uses automated backups and a final snapshot, with Multi-AZ as a production availability upgrade.
  #checkov:skip=CKV_AWS_161:The current Spring services authenticate with Secrets Manager-managed PostgreSQL credentials; IAM database authentication requires an application connection-provider migration.
  #checkov:skip=CKV_AWS_118:Enhanced Monitoring adds monitoring-stream cost; standard CloudWatch RDS metrics and alarms are enabled.
  #checkov:skip=CKV_AWS_353:Performance Insights is deferred to control cost until database workload baselines exist.
  #checkov:skip=CKV2_AWS_30:Full SQL statement logging may capture sensitive values and increase log ingestion; PostgreSQL and upgrade logs are exported without statement payload logging.
  backup_retention_period         = 7
  deletion_protection             = true
  skip_final_snapshot             = false
  final_snapshot_identifier       = "${local.name_prefix}-final"
  copy_tags_to_snapshot           = true
  auto_minor_version_upgrade      = true
  enabled_cloudwatch_logs_exports = ["postgresql", "upgrade"]
}
resource "aws_secretsmanager_secret" "api_database" {
  name = "${local.name_prefix}/database/video-api"
  #checkov:skip=CKV_AWS_149:Secrets Manager uses the AWS-managed service key by default; customer-managed KMS encryption adds recurring request charges.
  #checkov:skip=CKV2_AWS_57:Credential rotation requires a coordinated database password change and ECS task rollout; automated rotation is deferred until that flow is implemented.
}
resource "aws_secretsmanager_secret_version" "api_database" {
  secret_id     = aws_secretsmanager_secret.api_database.id
  secret_string = jsonencode({ username = "video_api", password = random_password.api_db.result, host = aws_db_instance.main.address, dbname = "fiapx" })
}
resource "aws_secretsmanager_secret" "notification_database" {
  name = "${local.name_prefix}/database/notification-worker"
  #checkov:skip=CKV_AWS_149:Secrets Manager uses the AWS-managed service key by default; customer-managed KMS encryption adds recurring request charges.
  #checkov:skip=CKV2_AWS_57:Credential rotation requires a coordinated database password change and ECS task rollout; automated rotation is deferred until that flow is implemented.
}
resource "aws_secretsmanager_secret_version" "notification_database" {
  secret_id     = aws_secretsmanager_secret.notification_database.id
  secret_string = jsonencode({ username = "notification_worker", password = random_password.notification_db.result, host = aws_db_instance.main.address, dbname = "fiapx_notifications" })
}
resource "aws_secretsmanager_secret" "jwt" {
  name = "${local.name_prefix}/jwt"
  #checkov:skip=CKV_AWS_149:Secrets Manager uses the AWS-managed service key by default; customer-managed KMS encryption adds recurring request charges.
  #checkov:skip=CKV2_AWS_57:JWT key rotation requires a coordinated key-id and public-key rollout across deployed clients; automated rotation is deferred until that flow exists.
}
resource "aws_secretsmanager_secret_version" "jwt" {
  secret_id     = aws_secretsmanager_secret.jwt.id
  secret_string = jsonencode({ private_key_base64 = var.jwt_private_key_base64, public_key_base64 = var.jwt_public_key_base64 })
}

resource "aws_mq_broker" "rabbitmq" {
  broker_name                = "${local.name_prefix}-mq"
  engine_type                = "RabbitMQ"
  engine_version             = "4.3"
  host_instance_type         = "mq.m7g.medium"
  deployment_mode            = "SINGLE_INSTANCE"
  publicly_accessible        = false
  subnet_ids                 = [aws_subnet.application[0].id]
  security_groups            = [aws_security_group.mq.id]
  auto_minor_version_upgrade = true
  encryption_options {
    use_aws_owned_key = true
  }
  logs { general = true }
  #checkov:skip=CKV_AWS_209:Amazon MQ remains encrypted with its AWS-owned KMS key; a customer-managed key adds recurring KMS charges.
  user {
    username = "fiapx"
    password = random_password.mq.result
  }
}
resource "random_password" "mq" {
  length  = 32
  special = false
}
resource "aws_secretsmanager_secret" "rabbitmq" {
  name = "${local.name_prefix}/rabbitmq"
  #checkov:skip=CKV_AWS_149:Secrets Manager uses the AWS-managed service key by default; customer-managed KMS encryption adds recurring request charges.
  #checkov:skip=CKV2_AWS_57:Broker password rotation requires a coordinated broker and ECS task rollout; automated rotation is deferred until that flow is implemented.
}
resource "aws_secretsmanager_secret_version" "rabbitmq" {
  secret_id     = aws_secretsmanager_secret.rabbitmq.id
  secret_string = jsonencode({ username = "fiapx", password = random_password.mq.result, host = local.mq_host, port = 5671 })
}

resource "aws_ses_domain_identity" "main" { domain = var.root_domain }
resource "aws_route53_record" "ses_verification" {
  zone_id = data.aws_route53_zone.root.zone_id
  name    = "_amazonses.${var.root_domain}"
  type    = "TXT"
  ttl     = 600
  records = [aws_ses_domain_identity.main.verification_token]
}
resource "aws_ses_domain_dkim" "main" { domain = aws_ses_domain_identity.main.domain }
resource "aws_route53_record" "ses_dkim" {
  count   = 3
  zone_id = data.aws_route53_zone.root.zone_id
  name    = "${aws_ses_domain_dkim.main.dkim_tokens[count.index]}._domainkey.${var.root_domain}"
  type    = "CNAME"
  ttl     = 600
  records = ["${aws_ses_domain_dkim.main.dkim_tokens[count.index]}.dkim.amazonses.com"]
}
resource "aws_iam_user" "ses_smtp" {
  name = "${local.name_prefix}-ses-smtp"
  #checkov:skip=CKV_AWS_273:SES SMTP authentication uses SMTP credentials derived from an IAM user's access key; the dedicated user is scoped to sending from this verified domain and is not used for console access.
}
resource "aws_iam_user_policy" "ses_smtp" {
  #checkov:skip=CKV_AWS_40:Amazon SES SMTP credentials are derived from this dedicated least-privilege IAM user; the policy is restricted to the verified sending identity.
  name   = "ses-send-email"
  user   = aws_iam_user.ses_smtp.name
  policy = jsonencode({ Version = "2012-10-17", Statement = [{ Effect = "Allow", Action = ["ses:SendRawEmail"], Resource = "arn:aws:ses:${var.aws_region}:${data.aws_caller_identity.current.account_id}:identity/${var.root_domain}" }] })
}
resource "aws_secretsmanager_secret" "smtp" {
  name = "${local.name_prefix}/ses-smtp"
  #checkov:skip=CKV_AWS_149:Secrets Manager uses the AWS-managed service key by default; customer-managed KMS encryption adds recurring request charges.
  #checkov:skip=CKV2_AWS_57:SMTP credential rotation requires a coordinated SES credential replacement and ECS rollout; automated rotation is deferred until that flow is implemented.
}
resource "aws_secretsmanager_secret_version" "smtp" {
  secret_id     = aws_secretsmanager_secret.smtp.id
  secret_string = jsonencode({ username = var.ses_smtp_username, password = var.ses_smtp_password, host = "email-smtp.${var.aws_region}.amazonaws.com", port = 587 })
}

resource "aws_prometheus_workspace" "main" { alias = "${local.name_prefix}-metrics" }
resource "aws_ecs_cluster" "main" {
  name = local.name_prefix
  setting {
    name  = "containerInsights"
    value = "enhanced"
  }
}
resource "aws_iam_role" "task_execution" {
  name               = "${local.name_prefix}-task-execution"
  assume_role_policy = data.aws_iam_policy_document.ecs_assume.json
}
resource "aws_iam_role" "database_bootstrap" {
  name               = "${local.name_prefix}-database-bootstrap-task"
  assume_role_policy = data.aws_iam_policy_document.ecs_assume.json
}
resource "aws_iam_role" "task" {
  for_each           = toset(local.service_names)
  name               = "${local.name_prefix}-${each.key}-task"
  assume_role_policy = data.aws_iam_policy_document.ecs_assume.json
}
data "aws_iam_policy_document" "ecs_assume" {
  statement {
    actions = ["sts:AssumeRole"]
    principals {
      type        = "Service"
      identifiers = ["ecs-tasks.amazonaws.com"]
    }
  }
}
resource "aws_iam_role_policy_attachment" "execution" {
  role       = aws_iam_role.task_execution.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy"
}
resource "aws_iam_role_policy" "secrets_execution" {
  name   = "read-runtime-secrets"
  role   = aws_iam_role.task_execution.id
  policy = jsonencode({ Version = "2012-10-17", Statement = [{ Effect = "Allow", Action = ["secretsmanager:GetSecretValue"], Resource = [aws_secretsmanager_secret.api_database.arn, aws_secretsmanager_secret.notification_database.arn, aws_secretsmanager_secret.rabbitmq.arn, aws_secretsmanager_secret.jwt.arn, aws_secretsmanager_secret.smtp.arn, aws_db_instance.main.master_user_secret[0].secret_arn] }] })
}
resource "aws_iam_role_policy" "database_bootstrap_secrets" {
  name   = "read-bootstrap-database-secrets"
  role   = aws_iam_role.database_bootstrap.id
  policy = jsonencode({ Version = "2012-10-17", Statement = [{ Effect = "Allow", Action = ["secretsmanager:GetSecretValue"], Resource = [aws_db_instance.main.master_user_secret[0].secret_arn, aws_secretsmanager_secret.api_database.arn, aws_secretsmanager_secret.notification_database.arn] }] })
}
resource "aws_iam_role_policy" "app_s3" {
  for_each = toset(["video-api", "video-processor"])
  name     = "media-bucket-access"
  role     = aws_iam_role.task[each.key].id
  policy = jsonencode({ Version = "2012-10-17", Statement = [
    { Effect = "Allow", Action = ["s3:GetObject", "s3:PutObject", "s3:DeleteObject", "s3:AbortMultipartUpload"], Resource = "${aws_s3_bucket.media.arn}/*" },
    { Effect = "Allow", Action = ["s3:ListBucket"], Resource = aws_s3_bucket.media.arn }
  ] })
}
resource "aws_iam_role_policy" "amp_write" {
  for_each = toset(local.service_names)
  name     = "amp-remote-write"
  role     = aws_iam_role.task[each.key].id
  policy   = jsonencode({ Version = "2012-10-17", Statement = [{ Effect = "Allow", Action = ["aps:RemoteWrite"], Resource = aws_prometheus_workspace.main.arn }] })
}

resource "aws_iam_role_policy" "task_ecr_pull" {
  name = "pull-service-images"
  role = aws_iam_role.task_execution.id
  policy = jsonencode({ Version = "2012-10-17", Statement = [
    { Effect = "Allow", Action = ["ecr:GetAuthorizationToken"], Resource = "*" },
    { Effect = "Allow", Action = ["ecr:BatchCheckLayerAvailability", "ecr:GetDownloadUrlForLayer", "ecr:BatchGetImage"], Resource = [for repository in aws_ecr_repository.services : repository.arn] }
  ] })
}

data "aws_iam_openid_connect_provider" "github" {
  url = "https://token.actions.githubusercontent.com"
}
resource "aws_iam_role" "github_plan" {
  name               = "${local.name_prefix}-github-plan"
  assume_role_policy = jsonencode({ Version = "2012-10-17", Statement = [{ Effect = "Allow", Action = "sts:AssumeRoleWithWebIdentity", Principal = { Federated = data.aws_iam_openid_connect_provider.github.arn }, Condition = { StringEquals = { "token.actions.githubusercontent.com:aud" = "sts.amazonaws.com" }, StringLike = { "token.actions.githubusercontent.com:sub" = "repo:fiap-postech-team/fiapx-video-platform:*" } } }] })
}
resource "aws_iam_role_policy_attachment" "github_plan_readonly" {
  role       = aws_iam_role.github_plan.name
  policy_arn = "arn:aws:iam::aws:policy/ReadOnlyAccess"
}
resource "aws_iam_role_policy" "github_plan_state" {
  name = "terraform-state-access"
  role = aws_iam_role.github_plan.id
  policy = jsonencode({ Version = "2012-10-17", Statement = [
    { Effect = "Allow", Action = ["s3:GetObject"], Resource = "arn:aws:s3:::${var.terraform_state_bucket_name}/fiapx-video-platform/production/terraform.tfstate" },
    { Effect = "Allow", Action = ["s3:GetObject", "s3:PutObject", "s3:DeleteObject"], Resource = "arn:aws:s3:::${var.terraform_state_bucket_name}/fiapx-video-platform/production/terraform.tfstate.tflock" },
    { Effect = "Allow", Action = ["s3:ListBucket"], Resource = "arn:aws:s3:::${var.terraform_state_bucket_name}", Condition = { StringLike = { "s3:prefix" = ["fiapx-video-platform/production/*"] } } },
    { Effect = "Allow", Action = ["kms:Decrypt", "kms:DescribeKey", "kms:Encrypt", "kms:GenerateDataKey"], Resource = "arn:aws:kms:${var.aws_region}:${data.aws_caller_identity.current.account_id}:key/*" }
  ] })
}
resource "aws_iam_role" "github_deploy" {
  name               = "${local.name_prefix}-github-deploy"
  assume_role_policy = jsonencode({ Version = "2012-10-17", Statement = [{ Effect = "Allow", Action = "sts:AssumeRoleWithWebIdentity", Principal = { Federated = data.aws_iam_openid_connect_provider.github.arn }, Condition = { StringEquals = { "token.actions.githubusercontent.com:aud" = "sts.amazonaws.com", "token.actions.githubusercontent.com:sub" = "repo:fiap-postech-team/fiapx-video-platform:environment:production" } } }] })
}
resource "aws_iam_role_policy" "github_deploy" {
  name = "deploy-platform"
  role = aws_iam_role.github_deploy.id
  policy = jsonencode({ Version = "2012-10-17", Statement = [
    { Effect = "Allow", Action = ["ecr:GetAuthorizationToken", "sts:GetCallerIdentity"], Resource = "*" },
    { Effect = "Allow", Action = ["acm:*", "amp:*", "application-autoscaling:*", "cloudfront:*", "cloudwatch:*", "ec2:*", "ecs:*", "ecr:*", "elasticloadbalancing:*", "kms:*", "mq:*", "rds:*", "route53:*", "s3:*", "secretsmanager:*", "ses:*", "sns:*", "logs:*", "tag:GetResources"], Resource = "*" },
    { Effect = "Allow", Action = ["iam:AttachRolePolicy", "iam:CreateRole", "iam:CreateUser", "iam:DeleteRole", "iam:DeleteRolePolicy", "iam:DeleteUser", "iam:DeleteUserPolicy", "iam:DetachRolePolicy", "iam:GetOpenIDConnectProvider", "iam:GetPolicy", "iam:GetPolicyVersion", "iam:GetRole", "iam:GetRolePolicy", "iam:GetUser", "iam:GetUserPolicy", "iam:ListAttachedRolePolicies", "iam:ListRolePolicies", "iam:ListUserPolicies", "iam:PutRolePolicy", "iam:PutUserPolicy", "iam:TagRole", "iam:TagUser", "iam:UntagRole", "iam:UntagUser", "iam:UpdateAssumeRolePolicy"], Resource = ["arn:aws:iam::*:role/${local.name_prefix}-*", "arn:aws:iam::*:user/${local.name_prefix}-*", "arn:aws:iam::aws:policy/ReadOnlyAccess", "arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy", "arn:aws:iam::*:oidc-provider/token.actions.githubusercontent.com"] },
    { Effect = "Allow", Action = ["iam:ListOpenIDConnectProviders", "iam:ListRoles", "iam:ListUsers"], Resource = "*" },
    { Effect = "Allow", Action = ["iam:CreateServiceLinkedRole"], Resource = "*", Condition = { StringEquals = { "iam:AWSServiceName" = ["mq.amazonaws.com", "rds.amazonaws.com", "elasticloadbalancing.amazonaws.com", "ecs.amazonaws.com", "application-autoscaling.amazonaws.com"] } } },
    { Effect = "Allow", Action = ["ecr:BatchCheckLayerAvailability", "ecr:CompleteLayerUpload", "ecr:DescribeRepositories", "ecr:InitiateLayerUpload", "ecr:PutImage", "ecr:UploadLayerPart"], Resource = [for repository in aws_ecr_repository.services : repository.arn] },
    { Effect = "Allow", Action = ["ecs:DescribeClusters", "ecs:DescribeServices", "ecs:DescribeTaskDefinition", "ecs:ListTaskDefinitions", "ecs:RegisterTaskDefinition", "ecs:UpdateService", "ecs:DescribeTasks", "ecs:RunTask"], Resource = "*" },
    { Effect = "Allow", Action = ["iam:PassRole"], Resource = concat([aws_iam_role.task_execution.arn, aws_iam_role.database_bootstrap.arn], [for role in aws_iam_role.task : role.arn]) },
    { Effect = "Allow", Action = ["s3:PutObject", "s3:ListBucket", "cloudfront:CreateInvalidation", "cloudfront:GetDistribution"], Resource = [aws_s3_bucket.frontend.arn, "${aws_s3_bucket.frontend.arn}/*", aws_cloudfront_distribution.app.arn] }
  ] })
}

resource "aws_ecs_task_definition" "service" {
  for_each                 = toset(local.service_names)
  family                   = "${local.name_prefix}-${each.key}"
  requires_compatibilities = ["FARGATE"]
  network_mode             = "awsvpc"
  cpu                      = each.key == "video-processor" ? "2048" : each.key == "video-api" ? "512" : "256"
  memory                   = each.key == "video-processor" ? "4096" : each.key == "video-api" ? "1024" : "512"
  execution_role_arn       = aws_iam_role.task_execution.arn
  task_role_arn            = aws_iam_role.task[each.key].arn
  ephemeral_storage { size_in_gib = each.key == "video-processor" ? 50 : 21 }
  container_definitions = jsonencode([
    merge({
      name             = each.key
      image            = local.service_images[each.key]
      essential        = true
      portMappings     = [{ containerPort = local.service_ports[each.key], protocol = "tcp" }]
      logConfiguration = { logDriver = "awslogs", options = { "awslogs-group" = aws_cloudwatch_log_group.services[each.key].name, "awslogs-region" = var.aws_region, "awslogs-stream-prefix" = "ecs" } }
      environment = concat([
        { name = "RABBITMQ_HOST", value = local.mq_host },
        { name = "RABBITMQ_PORT", value = "5671" },
        { name = "SPRING_RABBITMQ_SSL_ENABLED", value = "true" },
        { name = "RABBITMQ_USER", value = "fiapx" },
        { name = "S3_BUCKET", value = aws_s3_bucket.media.id },
        { name = "S3_ENDPOINT", value = "" },
        { name = "APP_VIDEO_STORAGE_MODE", value = "s3" },
        { name = "AWS_REGION", value = var.aws_region }
        ], each.key == "video-api" ? [
        { name = "SPRING_PROFILES_ACTIVE", value = "prod" },
        { name = "DATABASE_URL", value = "jdbc:postgresql://${aws_db_instance.main.address}:5432/fiapx?sslmode=require" },
        { name = "DATABASE_USER", value = "video_api" },
        { name = "RABBITMQ_USER", value = "fiapx" },
        { name = "APP_AUTH_ISSUER", value = "https://app.${var.root_domain}" },
        { name = "APP_AUTH_AUDIENCE", value = "fiapx-video-platform" },
        { name = "APP_AUTH_KEY_ID", value = "fiapx-production-1" },
        { name = "APP_WEB_ALLOWED_ORIGINS", value = "https://app.${var.root_domain}" }
        ] : each.key == "notification-worker" ? [
        { name = "NOTIFICATION_DATABASE_URL", value = "jdbc:postgresql://${aws_db_instance.main.address}:5432/fiapx_notifications?sslmode=require" },
        { name = "DATABASE_USER", value = "notification_worker" },
        { name = "SMTP_HOST", value = "email-smtp.${var.aws_region}.amazonaws.com" },
        { name = "SMTP_PORT", value = "587" },
        { name = "SPRING_MAIL_PROPERTIES_MAIL_SMTP_AUTH", value = "true" },
        { name = "SPRING_MAIL_PROPERTIES_MAIL_SMTP_STARTTLS_ENABLE", value = "true" },
        { name = "NOTIFICATION_DEFAULT_RECIPIENT", value = var.default_notification_recipient },
        { name = "NOTIFICATION_FROM_ADDRESS", value = "noreply@${var.root_domain}" }
        ] : [
        { name = "S3_BUCKET", value = aws_s3_bucket.media.id },
        { name = "RABBITMQ_EXCHANGE", value = "video.events" },
        { name = "RABBITMQ_QUEUE", value = "video.processing.v1" }
      ])
      secrets = concat(
        each.key == "video-api" ? [
          { name = "DATABASE_PASSWORD", valueFrom = "${aws_secretsmanager_secret.api_database.arn}:password::" },
          { name = "APP_AUTH_PRIVATE_KEY_BASE64", valueFrom = "${aws_secretsmanager_secret.jwt.arn}:private_key_base64::" },
          { name = "APP_AUTH_PUBLIC_KEY_BASE64", valueFrom = "${aws_secretsmanager_secret.jwt.arn}:public_key_base64::" }
        ] : [],
        each.key == "notification-worker" ? [
          { name = "DATABASE_PASSWORD", valueFrom = "${aws_secretsmanager_secret.notification_database.arn}:password::" },
          { name = "SMTP_USER", valueFrom = "${aws_secretsmanager_secret.smtp.arn}:username::" },
          { name = "SMTP_PASSWORD", valueFrom = "${aws_secretsmanager_secret.smtp.arn}:password::" }
        ] : [],
        [{ name = "RABBITMQ_PASSWORD", valueFrom = "${aws_secretsmanager_secret.rabbitmq.arn}:password::" }]
      )
    }, each.key == "video-processor" ? { healthCheck = { command = ["CMD-SHELL", "wget -q -O - http://localhost:8081/actuator/health | grep -q UP"], interval = 30, timeout = 5, retries = 3, startPeriod = 60 } } : {}),
    {
      name      = "adot-collector"
      image     = var.collector_image
      essential = false
      command   = ["--config=/etc/ecs/ecs-amp.yaml"]
      environment = [
        { name = "AWS_REGION", value = var.aws_region },
        { name = "AMP_ENDPOINT", value = "${aws_prometheus_workspace.main.prometheus_endpoint}api/v1/remote_write" }
      ]
      logConfiguration = { logDriver = "awslogs", options = { "awslogs-group" = aws_cloudwatch_log_group.adot.name, "awslogs-region" = var.aws_region, "awslogs-stream-prefix" = each.key } }
    }
  ])
}

resource "aws_ecs_task_definition" "database_bootstrap" {
  family                   = "${local.name_prefix}-database-bootstrap"
  requires_compatibilities = ["FARGATE"]
  network_mode             = "awsvpc"
  cpu                      = "256"
  memory                   = "512"
  execution_role_arn       = aws_iam_role.task_execution.arn
  task_role_arn            = aws_iam_role.database_bootstrap.arn
  container_definitions = jsonencode([{
    name  = "database-bootstrap"
    image = "postgres:17-alpine"
    command = ["sh", "-c", <<-EOT
      set -eu
      until pg_isready -h "$PGHOST" -p "$PGPORT" -U "$PGUSER"; do sleep 3; done
      psql -v ON_ERROR_STOP=1 -d postgres -tAc "SELECT 1 FROM pg_roles WHERE rolname='video_api'" | grep -q 1 || psql -v ON_ERROR_STOP=1 -d postgres -c 'CREATE ROLE video_api LOGIN'
      psql -v ON_ERROR_STOP=1 -d postgres -tAc "SELECT 1 FROM pg_roles WHERE rolname='notification_worker'" | grep -q 1 || psql -v ON_ERROR_STOP=1 -d postgres -c 'CREATE ROLE notification_worker LOGIN'
      psql -v ON_ERROR_STOP=1 -d postgres -c "ALTER ROLE video_api WITH PASSWORD '$API_DB_PASSWORD'"
      psql -v ON_ERROR_STOP=1 -d postgres -c "ALTER ROLE notification_worker WITH PASSWORD '$NOTIFICATION_DB_PASSWORD'"
      psql -v ON_ERROR_STOP=1 -d postgres -tAc "SELECT 1 FROM pg_database WHERE datname='fiapx_notifications'" | grep -q 1 || createdb -O notification_worker fiapx_notifications
      psql -v ON_ERROR_STOP=1 -d postgres -c "ALTER DATABASE fiapx OWNER TO video_api"
      psql -v ON_ERROR_STOP=1 -d postgres -c "GRANT CONNECT ON DATABASE fiapx_notifications TO notification_worker"
    EOT
    ]
    environment = [
      { name = "PGHOST", value = aws_db_instance.main.address },
      { name = "PGPORT", value = "5432" },
      { name = "PGUSER", value = "dbadmin" }
    ]
    secrets = [
      { name = "PGPASSWORD", valueFrom = "${aws_db_instance.main.master_user_secret[0].secret_arn}:password::" },
      { name = "API_DB_PASSWORD", valueFrom = "${aws_secretsmanager_secret.api_database.arn}:password::" },
      { name = "NOTIFICATION_DB_PASSWORD", valueFrom = "${aws_secretsmanager_secret.notification_database.arn}:password::" }
    ]
    essential = true
    logConfiguration = {
      logDriver = "awslogs"
      options = {
        "awslogs-group"         = aws_cloudwatch_log_group.adot.name
        "awslogs-region"        = var.aws_region
        "awslogs-stream-prefix" = "database-bootstrap"
      }
    }
  }])
}

resource "aws_ecs_service" "service" {
  for_each                           = toset(local.service_names)
  name                               = each.key
  cluster                            = aws_ecs_cluster.main.id
  task_definition                    = aws_ecs_task_definition.service[each.key].arn
  desired_count                      = each.key == "video-api" ? var.api_desired_count : each.key == "video-processor" ? var.processor_desired_count : var.notification_desired_count
  launch_type                        = "FARGATE"
  platform_version                   = "LATEST"
  deployment_minimum_healthy_percent = 0
  deployment_maximum_percent         = 200
  deployment_circuit_breaker {
    enable   = true
    rollback = true
  }
  network_configuration {
    subnets          = aws_subnet.application[*].id
    security_groups  = concat([aws_security_group.ecs.id], each.key == "video-api" ? [aws_security_group.api.id] : [])
    assign_public_ip = false
  }
  dynamic "load_balancer" {
    for_each = each.key == "video-api" ? [1] : []
    content {
      target_group_arn = aws_lb_target_group.api.arn
      container_name   = "video-api"
      container_port   = 8080
    }
  }
  depends_on = [aws_lb_listener_rule.cloudfront_only, aws_iam_role_policy_attachment.execution, aws_iam_role_policy.secrets_execution]
}

resource "aws_appautoscaling_target" "processor" {
  max_capacity       = 3
  min_capacity       = var.processor_desired_count
  resource_id        = "service/${aws_ecs_cluster.main.name}/${aws_ecs_service.service["video-processor"].name}"
  scalable_dimension = "ecs:service:DesiredCount"
  service_namespace  = "ecs"
}
resource "aws_appautoscaling_policy" "processor_cpu" {
  name               = "${local.name_prefix}-processor-cpu"
  policy_type        = "TargetTrackingScaling"
  resource_id        = aws_appautoscaling_target.processor.resource_id
  scalable_dimension = aws_appautoscaling_target.processor.scalable_dimension
  service_namespace  = aws_appautoscaling_target.processor.service_namespace
  target_tracking_scaling_policy_configuration {
    target_value       = 70
    scale_in_cooldown  = 300
    scale_out_cooldown = 60
    predefined_metric_specification { predefined_metric_type = "ECSServiceAverageCPUUtilization" }
  }
}

resource "aws_cloudwatch_metric_alarm" "api_5xx" {
  alarm_name          = "${local.name_prefix}-alb-5xx"
  namespace           = "AWS/ApplicationELB"
  metric_name         = "HTTPCode_Target_5XX_Count"
  statistic           = "Sum"
  period              = 60
  evaluation_periods  = 5
  threshold           = 10
  comparison_operator = "GreaterThanOrEqualToThreshold"
  dimensions          = { LoadBalancer = aws_lb.api.arn_suffix }
  alarm_actions       = [aws_sns_topic.alerts.arn]
}
resource "aws_cloudwatch_metric_alarm" "api_cpu" {
  alarm_name          = "${local.name_prefix}-api-cpu"
  namespace           = "AWS/ECS"
  metric_name         = "CPUUtilization"
  statistic           = "Average"
  period              = 60
  evaluation_periods  = 5
  threshold           = 80
  comparison_operator = "GreaterThanOrEqualToThreshold"
  dimensions          = { ClusterName = aws_ecs_cluster.main.name, ServiceName = aws_ecs_service.service["video-api"].name }
  alarm_actions       = [aws_sns_topic.alerts.arn]
}
resource "aws_cloudwatch_metric_alarm" "rds_cpu" {
  alarm_name          = "${local.name_prefix}-rds-cpu"
  namespace           = "AWS/RDS"
  metric_name         = "CPUUtilization"
  statistic           = "Average"
  period              = 300
  evaluation_periods  = 3
  threshold           = 80
  comparison_operator = "GreaterThanOrEqualToThreshold"
  dimensions          = { DBInstanceIdentifier = aws_db_instance.main.id }
  alarm_actions       = [aws_sns_topic.alerts.arn]
}
resource "aws_cloudwatch_metric_alarm" "mq_cpu" {
  alarm_name          = "${local.name_prefix}-mq-cpu"
  namespace           = "AWS/AmazonMQ"
  metric_name         = "SystemCpuUtilization"
  statistic           = "Average"
  period              = 300
  evaluation_periods  = 3
  threshold           = 80
  comparison_operator = "GreaterThanOrEqualToThreshold"
  dimensions          = { Broker = aws_mq_broker.rabbitmq.broker_name }
  alarm_actions       = [aws_sns_topic.alerts.arn]
}
resource "aws_cloudwatch_metric_alarm" "mq_backlog" {
  alarm_name          = "${local.name_prefix}-mq-message-backlog"
  comparison_operator = "GreaterThanOrEqualToThreshold"
  evaluation_periods  = 3
  threshold           = 1000
  alarm_description   = "Aggregate RabbitMQ message count is elevated; inspect application queues and DLQs in AMP/RabbitMQ."
  alarm_actions       = [aws_sns_topic.alerts.arn]
  treat_missing_data  = "notBreaching"
  metric_query {
    id          = "m1"
    expression  = "SUM(SEARCH('{AWS/AmazonMQ,Broker,Node} MetricName=\"MessageCount\" Broker=\"${aws_mq_broker.rabbitmq.broker_name}\"', 'Maximum', 60))"
    label       = "Aggregate RabbitMQ messages"
    return_data = true
  }
}
resource "aws_sns_topic" "alerts" {
  name              = "${local.name_prefix}-alerts"
  kms_master_key_id = "alias/aws/sns"
}
resource "aws_sns_topic_subscription" "email" {
  topic_arn = aws_sns_topic.alerts.arn
  protocol  = "email"
  endpoint  = var.alert_email
}
