# Infraestrutura — fiap-feedback

## Visão Geral

O sistema é composto por três AWS Lambda Functions independentes, construídas com **Quarkus**, que se comunicam via **Amazon SQS** e persistem dados no **Amazon DynamoDB**. Notificações críticas são enviadas por e-mail via **SMTP**.

```
┌─────────────────────────────────────────────────────────────────────┐
│                           AWS Cloud                                  │
│                                                                      │
│   API Gateway ──► lambda-postar-feedback ──► DynamoDB (feedbacks)   │
│                         │                                            │
│                         └──────────────────► SQS (feedback-post)    │
│                                                    │                 │
│                         lambda-notificar-critico ◄─┘                │
│                                    │                                 │
│                                    └──────────────► SMTP (e-mail)   │
│                                                                      │
│   API Gateway ──► lambda-gerar-relatorio ──► DynamoDB (feedbacks)   │
└─────────────────────────────────────────────────────────────────────┘
```

---

## Módulos Maven

O projeto é um **multi-module Maven** com os seguintes módulos:

| Módulo                    | Tipo           | Descrição                                               |
|---------------------------|----------------|---------------------------------------------------------|
| `core`                    | Biblioteca     | Domínio puro — sem dependências de frameworks           |
| `infra-dynamodb`          | Biblioteca     | Adaptador de persistência para o AWS DynamoDB           |
| `infra-sqs`               | Biblioteca     | Adaptador de mensageria para o AWS SQS                  |
| `lambda-postar-feedback`  | Lambda/Quarkus | Entry point para receber e registrar feedbacks          |
| `lambda-gerar-relatorio`  | Lambda/Quarkus | Entry point para geração de relatórios periódicos       |
| `lambda-notificar-critico`| Lambda/Quarkus | Consumer SQS — envia e-mail para avaliações críticas    |

### Regra de Dependência entre Módulos

```
lambda-*  ──depends──►  core  ◄──depends──  infra-*
lambda-*  ──depends──►  infra-* (apenas os necessários)
```

Nenhum módulo `infra-*` ou `core` depende de `lambda-*`.

---

## Tecnologias e Versões

| Tecnologia          | Uso                                            |
|---------------------|------------------------------------------------|
| Java 21             | Linguagem e plataforma de execução             |
| Quarkus             | Framework para build e runtime das Lambdas     |
| AWS Lambda          | Plataforma de execução serverless              |
| Amazon DynamoDB     | Banco de dados NoSQL para persistência         |
| Amazon SQS          | Fila de mensagens para eventos assíncronos     |
| AWS SDK v2          | Acesso aos serviços AWS (`UrlConnectionHttpClient`) |
| Quarkus Mailer      | Envio de e-mails via SMTP                      |
| JBoss Logging       | Logging nos adaptadores e lambdas              |
| Jackson             | Serialização/Desserialização de JSON           |

---

## Adaptador de Persistência — `infra-dynamodb`

**Classe:** `AdaptadorRepositorioDynamoDB`  
**Implementa:** `IRepositorioFeedback`

### Tabela DynamoDB: `feedbacks`

| Atributo       | Tipo   | Descrição                                  |
|----------------|--------|--------------------------------------------|
| `id`           | String | UUID gerado no momento do `salvar()`        |
| `descricao`    | String | Texto da avaliação                          |
| `nota`         | Number | Pontuação de 1 a 5                          |
| `ehItemCritico`| Boolean| `true` se nota < 3                          |
| `criadoEm`     | String | Timestamp ISO-8601 (ex: `2024-01-15T10:30:00Z`) |

> **Região AWS:** `us-east-2` (configurado diretamente no cliente).

### Consulta por Período

A operação `obterFeedbacks(dataInicial, dataFinal)` realiza um **Scan** com `FilterExpression`:

```
criadoEm >= :inicio AND criadoEm <= :fim
```

O filtro funciona corretamente pois o campo `criadoEm` usa formato ISO-8601, que permite comparação lexicográfica como intervalo de datas.

### Configuração

| Propriedade                      | Padrão      | Descrição              |
|----------------------------------|-------------|------------------------|
| `dynamodb.tabela.feedbacks`      | `feedbacks` | Nome da tabela no DynamoDB |

---

## Adaptador de Mensageria — `infra-sqs`

**Classe:** `AdaptadorPublicadorSqs`  
**Implementa:** `IPublicadorEventos` e `IConsumidorEventos`

### Fila SQS: `feedback-post`

| Propriedade | Valor                                                              |
|-------------|--------------------------------------------------------------------|
| URL da Fila | `https://sqs.us-east-2.amazonaws.com/986119050927/feedback-post`   |
| Região      | `us-east-2`                                                         |

### Publicação de Evento

Serializa o objeto `FeedbackPostado` para JSON (Jackson) e envia como `messageBody` para a fila. Apenas instâncias de `FeedbackPostado` são aceitas; outros tipos geram log `WARN` e são ignorados.

### Consumo de Mensagem

Consome **1 mensagem por vez** (`maxNumberOfMessages=1`). Após o processamento, a mensagem é **deletada imediatamente** da fila via `receiptHandle`. Retorna `null` se a fila estiver vazia.

### Configuração

| Propriedade      | Descrição                           |
|------------------|-------------------------------------|
| `sqs.queue.url`  | URL completa da fila SQS (obrigatória) |

---

## Lambda Functions

### `lambda-postar-feedback`

**Handler Quarkus:** `postarFeedback`

**Responsabilidade:** Recebe um novo feedback, valida a autenticação, persiste no DynamoDB e publica evento na fila SQS.

**Dependências de infraestrutura:**
- `infra-dynamodb` (persistência)
- `infra-sqs` (publicação de evento)

**Configuração (`application.properties`):**

```properties
quarkus.lambda.handler=postarFeedback
sqs.queue.url=https://sqs.us-east-2.amazonaws.com/986119050927/feedback-post

quarkus.index-dependency.infra-sqs.group-id=br.com.luizen
quarkus.index-dependency.infra-sqs.artifact-id=infra-sqs
quarkus.index-dependency.infra-dynamodb.group-id=br.com.luizen
quarkus.index-dependency.infra-dynamodb.artifact-id=infra-dynamodb
```

---

### `lambda-gerar-relatorio`

**Handler Quarkus:** `gerarRelatorio`

**Responsabilidade:** Recebe um período (datas inicial e final), valida a autenticação e retorna as estatísticas calculadas dos feedbacks do período.

**Dependências de infraestrutura:**
- `infra-dynamodb` (consulta de feedbacks)

**Configuração (`application.properties`):**

```properties
quarkus.lambda.handler=gerarRelatorio

quarkus.index-dependency.infra-dynamodb.group-id=br.com.luizen
quarkus.index-dependency.infra-dynamodb.artifact-id=infra-dynamodb
```

---

### `lambda-notificar-critico`

**Handler Quarkus:** `notificarItemCritico`

**Responsabilidade:** Consome mensagens da fila SQS. Se o campo `ehItemCritico` do evento for `true`, envia um e-mail de alerta via SMTP.

**Dependências de infraestrutura:**
- `infra-sqs` (consumo de evento)
- `quarkus-mailer` (envio de e-mail)

**Configuração (`application.properties`):**

```properties
quarkus.lambda.handler=notificarItemCritico
sqs.queue.url=https://sqs.us-east-2.amazonaws.com/986119050927/feedback-post

# Destinatário das notificações
email.destinatario=seuemail@gmail.com

# Configurações SMTP (via variáveis de ambiente)
quarkus.mailer.from=${SMTP_REMETENTE:noreply@empresa.com}
quarkus.mailer.host=${SMTP_HOST}
quarkus.mailer.port=${SMTP_PORT:587}
quarkus.mailer.start-tls=REQUIRED
quarkus.mailer.username=${SMTP_USUARIO}
quarkus.mailer.password=${SMTP_SENHA}

# Perfil dev: mock SMTP (não envia e-mail real)
%dev.quarkus.mailer.mock=true
```

### Variáveis de Ambiente SMTP

| Variável          | Descrição                                    | Exemplo                          |
|-------------------|----------------------------------------------|----------------------------------|
| `SMTP_HOST`       | Endereço do servidor SMTP (obrigatório)       | `smtp.gmail.com`                 |
| `SMTP_PORT`       | Porta SMTP (padrão: `587`)                   | `587`                            |
| `SMTP_USUARIO`    | Usuário de autenticação SMTP (obrigatório)    | `usuario@gmail.com`              |
| `SMTP_SENHA`      | Senha ou App Password do SMTP (obrigatório)   | —                                |
| `SMTP_REMETENTE`  | Endereço do remetente (padrão: `noreply@empresa.com`) | `noreply@empresa.com` |

> **Segurança:** Nunca commitar credenciais SMTP no repositório. Use variáveis de ambiente na configuração da Lambda no AWS Console ou via AWS Secrets Manager.

---

## Build e Empacotamento

Cada módulo `lambda-*` usa o plugin `quarkus-maven-plugin` para gerar o artefato no formato esperado pelo AWS Lambda.

### Build JVM (padrão para desenvolvimento)

```bash
# A partir da raiz do projeto
./mvnw package -pl lambda-postar-feedback -am
./mvnw package -pl lambda-gerar-relatorio -am
./mvnw package -pl lambda-notificar-critico -am
```

O artefato gerado é um JAR com runner em `target/`.

### SAM (Serverless Application Model)

Cada módulo `lambda-*` possui os arquivos de configuração SAM gerados em `target/`:

| Arquivo              | Uso                                |
|----------------------|------------------------------------|
| `sam.jvm.yaml`       | Deploy em modo JVM                 |
| `sam.native.yaml`    | Deploy em modo nativo (GraalVM)    |
| `manage.sh`          | Script auxiliar de gerenciamento   |
| `bootstrap-example.sh` | Exemplo de bootstrap para modo nativo |

---

## Autenticação

Todas as operações de entrada (`lambda-postar-feedback` e `lambda-gerar-relatorio`) validam a requisição através de `ValidadorAutenticacao`, que compara o hash SHA-256 da `apiKey` recebida com um hash esperado armazenado em código.

> Consulte a seção de regras de negócio ([regras-de-negocio.md](regras-de-negocio.md)) para detalhes sobre o mecanismo de validação e como trocar a chave.

---

## Observabilidade (Logging)

Logs são emitidos apenas nos módulos `infra-*` e `lambda-*` usando `org.jboss.logging.Logger`. O módulo `core` não produz logs.

Consulte [guidelines-log.md](guidelines-log.md) para os padrões de nível e formato de log adotados no projeto.
