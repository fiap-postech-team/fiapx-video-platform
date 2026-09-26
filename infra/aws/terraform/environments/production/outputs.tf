output "app_url" { value = "https://app.${var.root_domain}" }
output "api_origin_url" { value = "https://origin.${var.root_domain}" }
output "frontend_bucket" { value = aws_s3_bucket.frontend.id }
output "cloudfront_distribution_id" { value = aws_cloudfront_distribution.app.id }
output "media_bucket" { value = aws_s3_bucket.media.id }
output "ecr_repository_urls" { value = { for name, repository in aws_ecr_repository.services : name => repository.repository_url } }
output "ecs_cluster_name" { value = aws_ecs_cluster.main.name }
output "ecs_service_names" { value = { for name, service in aws_ecs_service.service : name => service.name } }
output "prometheus_workspace_id" { value = aws_prometheus_workspace.main.id }
output "ecs_database_bootstrap_task_definition" { value = aws_ecs_task_definition.database_bootstrap.arn }
output "ecs_application_subnet_ids" { value = aws_subnet.application[*].id }
output "ecs_security_group_id" { value = aws_security_group.ecs.id }
output "github_plan_role_arn" { value = aws_iam_role.github_plan.arn }
output "github_deploy_role_arn" { value = aws_iam_role.github_deploy.arn }
