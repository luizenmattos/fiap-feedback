# Guidelines de Log — fiap-feedback

## Biblioteca de Log

O projeto usa **Quarkus** com suporte nativo ao **JBoss Logging**. Use sempre `org.jboss.logging.Logger` nos módulos `lambda-*` e `infra-*`.

```java
import org.jboss.logging.Logger;

private static final Logger LOG = Logger.getLogger(MinhaClasse.class);
```

> O `core` **não deve conter logs**. Logs são preocupações de infraestrutura. Entidades e casos de uso retornam dados (erros de validação, resultados) — quem decide registrar é a borda.

---

## Onde Logar

| Camada        | Deve logar? | Justificativa                                              |
|---------------|-------------|------------------------------------------------------------|
| `core`        | **Não**     | Domínio puro; observabilidade é responsabilidade da borda  |
| `infra-*`     | **Sim**     | Operações técnicas com risco de falha (I/O, rede, AWS)     |
| `lambda-*`    | **Sim**     | Ponto de entrada; registra início, fim e erros de fluxo    |

---

## Níveis de Log e Quando Usar

| Nível   | Quando usar                                                                 |
|---------|-----------------------------------------------------------------------------|
| `ERROR` | Falha inesperada que impede a operação de ser concluída. Sempre com `exception`. |
| `WARN`  | Situação anormal mas recuperável; algo que merece atenção mas não interrompeu o fluxo. |
| `INFO`  | Eventos relevantes do fluxo normal: início/fim de operação, resultado resumido. |
| `DEBUG` | Detalhes internos úteis para investigação em desenvolvimento (desabilitado em produção). |

---

## O Que Registrar em Cada Camada

### `lambda-*` — Entry Points

```java
@Override
public FeedbackOutput handleRequest(FeedbackInput input, Context context) {
    LOG.infof("Iniciando postagem de feedback. nota=%d", input.nota);

    List<String> erros = PostarFeedback.executar(
            input.descricao, input.nota, publicadorEventos, repositorioFeedback);

    if (erros != null) {
        LOG.warnf("Feedback rejeitado por erros de validação: %s", erros);
        return FeedbackOutput.feedbackComErro(erros);
    }

    LOG.info("Feedback postado com sucesso.");
    return FeedbackOutput.feedbackPostado(input.nota.toString());
}
```

**Registre:**
- `INFO` ao iniciar o handler com parâmetros não-sensíveis (nota, período de data)
- `WARN` quando o domínio retornar erros de validação
- `INFO` ao concluir com sucesso
- `ERROR` ao capturar exceções inesperadas (veja padrão abaixo)

### `infra-*` — Adaptadores

```java
@Override
public void salvar(Feedback feedback) {
    LOG.debugf("Salvando feedback no DynamoDB. tabela=%s", nomeTabela);
    try {
        dynamoDbClient.putItem(requisicao);
        LOG.info("Feedback salvo no DynamoDB com sucesso.");
    } catch (Exception e) {
        LOG.errorf(e, "Falha ao salvar feedback no DynamoDB. tabela=%s", nomeTabela);
        throw new RuntimeException("Falha ao salvar feedback no DynamoDB: " + e.getMessage(), e);
    }
}
```

**Registre:**
- `DEBUG` antes de operações de I/O com dados contextuais (tabela, fila, IDs)
- `INFO` na conclusão bem-sucedida de operações relevantes
- `ERROR` + stack trace em qualquer exceção capturada antes de relançar

---

## Padrão para Log de Erro com Exception

Sempre passe a exceção como **primeiro argumento** do método de log para garantir que o stack trace apareça no output:

```java
// CORRETO — stack trace incluído
LOG.errorf(e, "Falha ao publicar evento no SQS. fila=%s", urlFila);

// ERRADO — stack trace perdido
LOG.error("Falha ao publicar evento no SQS: " + e.getMessage());
```

---

## Dados Sensíveis — O Que Nunca Logar

Nunca inclua nos logs:

- Conteúdo completo da `descricao` do feedback (pode conter dados pessoais — LGPD)
- Credenciais, tokens ou chaves de API
- Detalhes internos de exceções do AWS SDK que contenham ARN ou dados de conta em produção

**Regra prática:** log de entrada registra apenas metadados (`nota`, `dataInicial`, `dataFinal`, tamanho da lista) — nunca o payload completo.

```java
// CORRETO
LOG.infof("Iniciando geração de relatório. periodo=%s a %s", input.dataInicial, input.dataFinal);

// EVITAR em produção
LOG.infof("Input completo: %s", input.toString());
```

---

## Contexto Obrigatório nas Mensagens

Toda mensagem de log deve responder: **o quê** aconteceu + **onde** (recurso técnico envolvido) + **resultado**.

| Elemento     | Exemplo                             |
|--------------|-------------------------------------|
| O quê        | "Salvando feedback", "Publicando evento" |
| Onde         | `tabela=feedbacks`, `fila=https://...` |
| Resultado    | "com sucesso", "falhou com erro X"  |

---

## Configuração por Ambiente

Defina o nível de log no `application.properties` de cada Lambda:

```properties
# Produção — apenas INFO e acima
quarkus.log.level=INFO
quarkus.log.category."br.com.luizen".level=INFO

# Desenvolvimento local — DEBUG ativo
%dev.quarkus.log.level=DEBUG
%dev.quarkus.log.category."br.com.luizen".level=DEBUG
```

> Use **perfis do Quarkus** (`%dev`, `%test`, `%prod`) para não precisar alterar código entre ambientes.

---

## Checklist de Revisão de Log

Antes de abrir um PR, valide:

- [ ] Nenhum `System.out.println` ou `e.printStackTrace()` no código
- [ ] `core` não importa nenhuma biblioteca de log
- [ ] Erros em `infra-*` passam a exceção como primeiro argumento do `LOG.error`
- [ ] Nenhum dado pessoal ou credencial registrado nos logs
- [ ] Handler da Lambda loga início (`INFO`) e erros de validação (`WARN`)
- [ ] Nível `DEBUG` não habilitado nos `application.properties` de produção
