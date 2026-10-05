# Module 6 – Task app (Spring Boot on ECS Fargate)

A simple full-stack task list for creating, viewing, updating and deleting tasks. It runs on ECS Fargate.
Infrastructure (CloudFormation, Git sync) lives in **[Module-6-infra](https://github.com/Michelle-anyika/Module-6-infra)**.

## What it shows

| Concern | How |
|---|---|
| **Reads accelerated by Redis** | Cache-aside in `TaskService.findAll()`. It reads `tasks:all` from ElastiCache first. On a miss it loads from RDS and caches for 5 minutes. The UI badge and the `X-Data-Source` header (`REDIS` / `RDS`) show which source served each read, with timing. |
| **Writes persisted to RDS through RDS Proxy** | Spring Data JPA → HikariCP → PostgreSQL JDBC → **RDS Proxy** endpoint. Every write evicts the cache. |
| **AWS SDK for backend auth** | `IamAuthPgDataSource` uses AWS SDK v2 `RdsUtilities.generateAuthenticationToken` with the ECS task role. The proxy requires IAM auth + TLS, so no DB password reaches the app. |
| **Redis over TLS + AUTH** | Lettuce with `spring.data.redis.ssl.enabled=true`. The AUTH token comes from Secrets Manager as an ECS secret. |
| **Graceful degradation** | If Redis is unavailable, reads fall back to RDS instead of failing. |
| **Blue/green visibility** | The page footer shows the serving task's hostname and image SHA. |

Endpoints: `/` (UI), `/api/tasks` (REST: GET, POST, GET/PUT/DELETE `/{id}`), `/health` (ALB and container health checks).

## Build and deploy

`.github/workflows/build-and-push.yml` runs on every push to `main`:

1. `mvn verify` (JDK 21).
2. Assumes `module-6-dev-github-actions` through **GitHub OIDC** (`id-token: write`). No stored AWS keys.
3. Builds the multi-stage `Dockerfile` (non-root JRE 21 Alpine runtime).
4. Renders `ecs/taskdef.json` + `ecs/appspec.yaml` and uploads `deploy/bundle.zip` to the pipeline artifact bucket.
5. Pushes `:<sha>`, then `:latest` to ECR. EventBridge sees the push and starts CodePipeline, which runs a CodeDeploy blue/green release.

### Repository variables (Settings → Secrets and variables → Actions → Variables)

| Variable | Value |
|---|---|
| `AWS_REGION` | `eu-north-1` |
| `AWS_ROLE_ARN` | `arn:aws:iam::615299752577:role/module-6-dev-github-actions` |
| `ECR_REPOSITORY` | `module-6-dev-app` |
| `ARTIFACT_BUCKET` | `module-6-dev-artifacts-615299752577-eu-north-1` |
| `TASK_EXECUTION_ROLE_ARN` | `arn:aws:iam::615299752577:role/module-6-dev-ecs-execution-role` |
| `TASK_ROLE_ARN` | `arn:aws:iam::615299752577:role/module-6-dev-ecs-task-role` |
| `DB_PROXY_ENDPOINT` | `module-6-dev-proxy.proxy-c96sacsq4cs3.eu-north-1.rds.amazonaws.com` |
| `DB_NAME` / `DB_USER` | `module6` / `module6admin` |
| `REDIS_ENDPOINT` | `master.module-6-dev-redis.9zfvrc.eun1.cache.amazonaws.com` |
| `REDIS_SECRET_ARN` | `arn:aws:secretsmanager:eu-north-1:615299752577:secret:module-6/dev/redis-auth-B0vNOb` |

None of these are secrets. They are identifiers, so they are stored as variables.

## Run locally

```bash
docker run -d -p 5432:5432 -e POSTGRES_DB=module6 -e POSTGRES_PASSWORD=postgres postgres:16
docker run -d -p 6379:6379 redis:7
mvn spring-boot:run        # http://localhost:8080 (DB_IAM_AUTH=false by default)
```
