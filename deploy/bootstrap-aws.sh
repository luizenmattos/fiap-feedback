#!/usr/bin/env bash
# ──────────────────────────────────────────────────────────────────────────────
# deploy/bootstrap-aws.sh
#
# Script de configuração ONE-TIME da AWS para habilitar o pipeline de CI/CD.
# Execute uma única vez antes do primeiro push para a branch main.
#
# Pré-requisitos:
#   - AWS CLI instalado e configurado com permissão de administrador
#   - jq instalado (usado para extrair valores do JSON)
#
# Uso:
#   chmod +x deploy/bootstrap-aws.sh
#   ./deploy/bootstrap-aws.sh <owner/repo-github> <aws-region> <bucket-suffix>
#
# Exemplo:
#   ./deploy/bootstrap-aws.sh meu-usuario/fiap-feedback us-east-2 fiap-123
# ──────────────────────────────────────────────────────────────────────────────
set -euo pipefail

GITHUB_REPO="${1:-}"
AWS_REGION="${2:-us-east-2}"
BUCKET_SUFFIX="${3:-}"

if [[ -z "$GITHUB_REPO" || -z "$BUCKET_SUFFIX" ]]; then
  echo "Uso: $0 <owner/repo-github> <aws-region> <bucket-suffix>"
  echo "Exemplo: $0 meu-usuario/fiap-feedback us-east-2 fiap-123"
  exit 1
fi

BUCKET_NAME="sam-artifacts-${BUCKET_SUFFIX}"
ROLE_NAME="GitHubActions-FiapFeedback-DeployRole"
STACK_NOME="fiap-feedback"

AWS_ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)
echo "Conta AWS: ${AWS_ACCOUNT_ID} | Região: ${AWS_REGION}"

# ── 1. Bucket S3 para artefatos do SAM ────────────────────────────────────────
echo ""
echo "[1/3] Criando bucket S3 para artefatos SAM: ${BUCKET_NAME}"

if aws s3api head-bucket --bucket "${BUCKET_NAME}" 2>/dev/null; then
  echo "  → Bucket já existe, pulando criação."
else
  if [[ "${AWS_REGION}" == "us-east-1" ]]; then
    aws s3api create-bucket \
      --bucket "${BUCKET_NAME}" \
      --region "${AWS_REGION}"
  else
    aws s3api create-bucket \
      --bucket "${BUCKET_NAME}" \
      --region "${AWS_REGION}" \
      --create-bucket-configuration LocationConstraint="${AWS_REGION}"
  fi

  # Bloquear acesso público ao bucket de artefatos
  aws s3api put-public-access-block \
    --bucket "${BUCKET_NAME}" \
    --public-access-block-configuration \
      BlockPublicAcls=true,IgnorePublicAcls=true,BlockPublicPolicy=true,RestrictPublicBuckets=true

  # Habilitar versionamento para facilitar rollback de artefatos
  aws s3api put-bucket-versioning \
    --bucket "${BUCKET_NAME}" \
    --versioning-configuration Status=Enabled

  echo "  → Bucket criado com acesso público bloqueado e versionamento ativo."
fi

# ── 2. OIDC Provider do GitHub Actions ────────────────────────────────────────
echo ""
echo "[2/3] Configurando OIDC Provider para GitHub Actions"

OIDC_URL="https://token.actions.githubusercontent.com"
OIDC_ARN="arn:aws:iam::${AWS_ACCOUNT_ID}:oidc-provider/token.actions.githubusercontent.com"

if aws iam get-open-id-connect-provider --open-id-connect-provider-arn "${OIDC_ARN}" 2>/dev/null; then
  echo "  → OIDC Provider já existe, pulando criação."
else
  # Thumbprint do OIDC do GitHub (valor estável, pode ser verificado em:
  # https://docs.aws.amazon.com/IAM/latest/UserGuide/id_roles_providers_create_oidc_verify-thumbprint.html)
  THUMBPRINT="6938fd4d98bab03faadb97b34396831e3780aea1"

  aws iam create-open-id-connect-provider \
    --url "${OIDC_URL}" \
    --client-id-list "sts.amazonaws.com" \
    --thumbprint-list "${THUMBPRINT}"

  echo "  → OIDC Provider criado."
fi

# ── 3. IAM Role para o GitHub Actions ─────────────────────────────────────────
echo ""
echo "[3/3] Criando IAM Role: ${ROLE_NAME}"

# Política de confiança — restringe a role à branch main do repositório informado
TRUST_POLICY=$(cat <<EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Principal": {
        "Federated": "arn:aws:iam::${AWS_ACCOUNT_ID}:oidc-provider/token.actions.githubusercontent.com"
      },
      "Action": "sts:AssumeRoleWithWebIdentity",
      "Condition": {
        "StringEquals": {
          "token.actions.githubusercontent.com:aud": "sts.amazonaws.com"
        },
        "StringLike": {
          "token.actions.githubusercontent.com:sub": "repo:${GITHUB_REPO}:ref:refs/heads/main"
        }
      }
    }
  ]
}
EOF
)

# Política de permissões — escopo mínimo para build e deploy via SAM/CloudFormation
DEPLOY_POLICY=$(cat <<EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "CloudFormationDeploy",
      "Effect": "Allow",
      "Action": [
        "cloudformation:CreateStack",
        "cloudformation:UpdateStack",
        "cloudformation:DescribeStacks",
        "cloudformation:DescribeStackEvents",
        "cloudformation:GetTemplate",
        "cloudformation:ValidateTemplate",
        "cloudformation:CreateChangeSet",
        "cloudformation:ExecuteChangeSet",
        "cloudformation:DescribeChangeSet"
      ],
      "Resource": "arn:aws:cloudformation:${AWS_REGION}:${AWS_ACCOUNT_ID}:stack/${STACK_NOME}*"
    },
    {
      "Sid": "CloudFormationTransform",
      "Effect": "Allow",
      "Action": "cloudformation:CreateChangeSet",
      "Resource": "arn:aws:cloudformation:${AWS_REGION}:aws:transform/Serverless-2016-10-31"
    },
    {
      "Sid": "LambdaDeploy",
      "Effect": "Allow",
      "Action": [
        "lambda:CreateFunction",
        "lambda:UpdateFunctionCode",
        "lambda:UpdateFunctionConfiguration",
        "lambda:GetFunction",
        "lambda:DeleteFunction",
        "lambda:AddPermission",
        "lambda:RemovePermission",
        "lambda:GetPolicy",
        "lambda:TagResource"
      ],
      "Resource": "arn:aws:lambda:${AWS_REGION}:${AWS_ACCOUNT_ID}:function:${STACK_NOME}*"
    },
    {
      "Sid": "S3Artefatos",
      "Effect": "Allow",
      "Action": [
        "s3:PutObject",
        "s3:GetObject",
        "s3:ListBucket"
      ],
      "Resource": [
        "arn:aws:s3:::${BUCKET_NAME}",
        "arn:aws:s3:::${BUCKET_NAME}/*"
      ]
    },
    {
      "Sid": "DynamoDBDeploy",
      "Effect": "Allow",
      "Action": [
        "dynamodb:CreateTable",
        "dynamodb:DescribeTable",
        "dynamodb:UpdateTable",
        "dynamodb:DeleteTable",
        "dynamodb:TagResource"
      ],
      "Resource": "arn:aws:dynamodb:${AWS_REGION}:${AWS_ACCOUNT_ID}:table/feedbacks*"
    },
    {
      "Sid": "SQSDeploy",
      "Effect": "Allow",
      "Action": [
        "sqs:CreateQueue",
        "sqs:GetQueueAttributes",
        "sqs:SetQueueAttributes",
        "sqs:DeleteQueue",
        "sqs:TagQueue"
      ],
      "Resource": "arn:aws:sqs:${AWS_REGION}:${AWS_ACCOUNT_ID}:feedback-post*"
    },
    {
      "Sid": "IAMParaLambda",
      "Effect": "Allow",
      "Action": [
        "iam:CreateRole",
        "iam:AttachRolePolicy",
        "iam:DetachRolePolicy",
        "iam:GetRole",
        "iam:DeleteRole",
        "iam:PassRole",
        "iam:PutRolePolicy",
        "iam:DeleteRolePolicy",
        "iam:TagRole"
      ],
      "Resource": "arn:aws:iam::${AWS_ACCOUNT_ID}:role/${STACK_NOME}*"
    },
    {
      "Sid": "APIGatewayDeploy",
      "Effect": "Allow",
      "Action": [
        "apigateway:GET",
        "apigateway:POST",
        "apigateway:PUT",
        "apigateway:DELETE",
        "apigateway:PATCH"
      ],
      "Resource": "arn:aws:apigateway:${AWS_REGION}::/*"
    },
    {
      "Sid": "EventsDeploy",
      "Effect": "Allow",
      "Action": [
        "events:PutRule",
        "events:DeleteRule",
        "events:DescribeRule",
        "events:PutTargets",
        "events:RemoveTargets"
      ],
      "Resource": "arn:aws:events:${AWS_REGION}:${AWS_ACCOUNT_ID}:rule/*"
    }
  ]
}
EOF
)

ROLE_ARN=$(aws iam get-role --role-name "${ROLE_NAME}" --query "Role.Arn" --output text 2>/dev/null || echo "")

if [[ -n "${ROLE_ARN}" ]]; then
  echo "  → Role já existe: ${ROLE_ARN}"
  echo "  → Atualizando política de confiança..."
  aws iam update-assume-role-policy \
    --role-name "${ROLE_NAME}" \
    --policy-document "${TRUST_POLICY}"
else
  ROLE_ARN=$(aws iam create-role \
    --role-name "${ROLE_NAME}" \
    --assume-role-policy-document "${TRUST_POLICY}" \
    --query "Role.Arn" \
    --output text)
  echo "  → Role criada: ${ROLE_ARN}"
fi

# Aplicar a política inline de deploy
aws iam put-role-policy \
  --role-name "${ROLE_NAME}" \
  --policy-name "FiapFeedbackDeployPolicy" \
  --policy-document "${DEPLOY_POLICY}"

echo "  → Política de permissões aplicada."

# ── Resumo ─────────────────────────────────────────────────────────────────────
echo ""
echo "══════════════════════════════════════════════════════════════════════"
echo "Bootstrap concluído! Configure os seguintes Secrets no GitHub:"
echo ""
echo "  AWS_DEPLOY_ROLE_ARN  →  ${ROLE_ARN}"
echo "  AWS_SAM_BUCKET       →  ${BUCKET_NAME}"
echo "  SMTP_HOST            →  <endereço do seu servidor SMTP>"
echo "  SMTP_PORT            →  587"
echo "  SMTP_USUARIO         →  <usuário SMTP>"
echo "  SMTP_SENHA           →  <senha SMTP>"
echo "  SMTP_REMETENTE       →  noreply@empresa.com"
echo "  EMAIL_DESTINATARIO   →  <email que receberá os alertas>"
echo ""
echo "Caminho para configurar Secrets:"
echo "  GitHub → Settings → Secrets and variables → Actions → New repository secret"
echo "══════════════════════════════════════════════════════════════════════"
