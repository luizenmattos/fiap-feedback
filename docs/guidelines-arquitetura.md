# Guidelines de Arquitetura — fiap-feedback

## Visão Geral

O projeto adota **Arquitetura Hexagonal** (Ports & Adapters) organizada em módulos Maven. O objetivo central é isolar as **regras de negócio** de qualquer detalhe tecnológico (banco de dados, filas, framework, protocolos de entrada).

```
┌──────────────────────────────────────────────────────┐
│                  Entry Points (Lambdas)               │
│  lambda-postar-feedback  lambda-gerar-relatorio       │
│  lambda-notificar-critico                             │
└───────────────────┬──────────────────────────────────┘
                    │ invoca casos de uso via
                    ▼
┌──────────────────────────────────────────────────────┐
│                      CORE                             │
│  Entidades · Casos de Uso · Portas (interfaces)       │
└────────────────┬─────────────────────────────────────┘
                 │ implementado por
                 ▼
┌──────────────────────────────────────────────────────┐
│               Infraestrutura (Adaptadores)            │
│  infra-dynamodb (persistência)  infra-sqs (eventos)   │
└──────────────────────────────────────────────────────┘
```

---

## Regra de Dependência

> **A seta de dependência aponta sempre para o `core`. Nunca o contrário.**

| Módulo                 | Pode depender de              | Jamais depende de                          |
|------------------------|-------------------------------|--------------------------------------------|
| `core`                 | — (nenhum módulo interno)     | `infra-*`, `lambda-*`, frameworks externos |
| `infra-dynamodb`       | `core`                        | `lambda-*`, `infra-sqs`                    |
| `infra-sqs`            | `core`                        | `lambda-*`, `infra-dynamodb`               |
| `lambda-*`             | `core`, `infra-*` necessários | outros `lambda-*`                          |

Violações desta regra quebram o isolamento do domínio e devem ser tratadas como **bugs de arquitetura**.

---

## Módulo `core` — Regras de Negócio

### Responsabilidade
Contém **tudo que representa o negócio**: entidades, objetos de valor, casos de uso e definição de contratos (portas). É a parte mais estável do sistema.

### O que pertence ao `core`

| Tipo              | Exemplos                                     | Localização                          |
|-------------------|----------------------------------------------|--------------------------------------|
| Entidade          | `Feedback`                                   | `br.com.luizen.core`                 |
| Objeto de Valor   | `RelatorioPeriodico`, `FeedbackPostado`      | `br.com.luizen.core`                 |
| Caso de Uso       | `PostarFeedback`, `GerarRelatorioPeriodico`  | `br.com.luizen.core`                 |
| Porta (interface) | `IRepositorioFeedback`, `IPublicadorEventos` | `br.com.luizen.core.ports`           |

### Restrições obrigatórias do `core`

1. **Zero dependência de framework** — sem `@Inject`, `@ApplicationScoped`, Quarkus, Spring, etc.
2. **Zero import de infraestrutura** — sem AWS SDK, JPA, SQS, DynamoDB.
3. **Casos de uso são métodos estáticos ou POJOs** — injeção de dependência é responsabilidade da borda, não do core.
4. **Portas são interfaces Java puras** — definem *o que* o domínio precisa, sem especificar *como* será implementado.
5. **Validação acontece no `core`** — a entidade `Feedback.validar()` é a fonte de verdade para regras de integridade.

### Convenções de nomenclatura

- Entidades e objetos de valor: substantivo em PascalCase → `Feedback`, `RelatorioPeriodico`
- Casos de uso: verbo no infinitivo em PascalCase → `PostarFeedback`, `GerarRelatorioPeriodico`
- Interfaces de porta: prefixo `I` + substantivo → `IRepositorioFeedback`, `IPublicadorEventos`
- Pacote de portas: `br.com.luizen.core.ports`

### Exemplo de caso de uso correto

```java
// core — sem anotações de framework, portas injetadas como parâmetros
public class PostarFeedback {

    public static List<String> executar(
            String descricao,
            Long nota,
            IPublicadorEventos publicadorEventos,
            IRepositorioFeedback repositorioFeedback) {

        Feedback feedback = Feedback.criar(descricao, nota);

        List<String> erros = feedback.validar();
        if (erros != null) return erros;

        repositorioFeedback.salvar(feedback);
        publicadorEventos.publicar(new FeedbackPostado(feedback));

        return null;
    }
}
```

---

## Módulo `infra-*` — Adaptadores de Saída

### Responsabilidade
Implementam as portas definidas no `core` usando tecnologias concretas.

### Regras

1. **Implementam exatamente uma ou mais interface(s) de porta do `core`**.
2. **Recebem configuração via `@ConfigProperty`** — sem valores de ambiente hardcoded.
3. **Usam `@ApplicationScoped`** — o Quarkus gerencia o ciclo de vida.
4. **Nunca lançam exceções de domínio** — em caso de falha técnica, envolva em `RuntimeException` com mensagem descritiva.
5. **Nomenclatura**: `Adaptador` + responsabilidade + tecnologia → `AdaptadorRepositorioDynamoDB`, `AdaptadorPublicadorSqs`.

### Exemplo de adaptador correto

```java
@ApplicationScoped
public class AdaptadorRepositorioDynamoDB implements IRepositorioFeedback {

    private final DynamoDbClient dynamoDbClient;
    private final String nomeTabela;

    public AdaptadorRepositorioDynamoDB(
            @ConfigProperty(name = "dynamodb.tabela.feedbacks", defaultValue = "feedbacks") String nomeTabela) {
        this.dynamoDbClient = DynamoDbClient.builder()
                .region(Region.US_EAST_2)
                .httpClientBuilder(UrlConnectionHttpClient.builder())
                .build();
        this.nomeTabela = nomeTabela;
    }

    @Override
    public void salvar(Feedback feedback) { /* ... */ }

    @Override
    public List<Feedback> obterFeedbacks(Date dataInicial, Date dataFinal) { /* ... */ }
}
```

---

## Módulos `lambda-*` — Adaptadores de Entrada

### Responsabilidade
São os **pontos de entrada** do sistema. Recebem eventos externos (HTTP, SQS, schedule), convertem para o formato do `core` e devolvem respostas.

### Regras

1. **Nenhuma lógica de negócio** — apenas conversão de dados e invocação do caso de uso.
2. **Usam `@Named` e `@Inject`** para identificação e injeção das portas.
3. **Definem DTOs próprios** de entrada e saída (`*Input`, `*Output`) — nunca expõem entidades do `core` diretamente.
4. **Tratam erros na borda** — validações retornadas pelo core são transformadas em resposta de saída adequada.
5. **Nomenclatura do handler**: `@Named("camelCase")` coincide com o `quarkus.lambda.handler` definido no `application.properties`.

### Estrutura de pacotes

```
lambda-postar-feedback/
  src/main/java/br/com/luizen/lambda/postarFeedback/
    PostarFeedbackLambda.java   ← handler principal
    FeedbackInput.java          ← DTO de entrada
    FeedbackOutput.java         ← DTO de saída
  src/main/resources/
    application.properties      ← handler + indexação dos módulos infra
```

### Exemplo de Lambda correto

```java
@Named("postarFeedback")
public class PostarFeedbackLambda implements RequestHandler<FeedbackInput, FeedbackOutput> {

    @Inject IPublicadorEventos publicadorEventos;
    @Inject IRepositorioFeedback repositorioFeedback;

    @Override
    public FeedbackOutput handleRequest(FeedbackInput input, Context context) {
        List<String> erros = PostarFeedback.executar(
                input.descricao, input.nota, publicadorEventos, repositorioFeedback);

        if (erros != null) return FeedbackOutput.feedbackComErro(erros);
        return FeedbackOutput.feedbackPostado(input.nota.toString());
    }
}
```

---

## Como Adicionar um Novo Caso de Uso

1. **Defina a porta** necessária em `core/src/main/java/.../core/ports/` (se ainda não existir).
2. **Implemente o caso de uso** em `core/src/main/java/.../core/` como classe com método estático (sem framework).
3. **Implemente o adaptador** no módulo `infra-*` correspondente, anotando com `@ApplicationScoped`.
4. **Crie ou atualize a Lambda** no módulo `lambda-*`, injetando a porta e invocando o caso de uso.
5. **Atualize o `application.properties`** da Lambda para indexar eventuais novos módulos infra.
6. **Escreva testes unitários no `core`** passando mocks das portas — sem subir Quarkus.

---

## Como Adicionar um Novo Adaptador de Infraestrutura

1. Crie um novo módulo Maven (ex: `infra-redis`).
2. Declare a dependência do `core` no `pom.xml` do novo módulo.
3. Implemente as interfaces de porta necessárias com `@ApplicationScoped`.
4. Adicione o módulo como dependência na Lambda que precisar.
5. No `application.properties` da Lambda, adicione o pacote para indexação do Quarkus CDI:
   ```properties
   quarkus.index-dependency.<alias>.group-id=br.com.luizen
   quarkus.index-dependency.<alias>.artifact-id=infra-redis
   ```

---

## Diretrizes de Teste

| Camada        | Tipo de Teste          | Estratégia                                              |
|---------------|------------------------|---------------------------------------------------------|
| `core`        | Unitário               | Instancia classes diretamente, usa mocks para portas    |
| `infra-*`     | Integração             | Usa LocalStack ou Testcontainers para AWS               |
| `lambda-*`    | Integração / E2E       | `@QuarkusTest` com injeção de mocks dos adaptadores     |

**Nunca suba dependências de AWS reais nos testes automatizados.**
