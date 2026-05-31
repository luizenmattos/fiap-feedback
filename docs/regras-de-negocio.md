# Regras de Negócio — fiap-feedback

## Visão Geral do Domínio

O sistema **fiap-feedback** permite que usuários registrem avaliações (feedbacks) sobre produtos ou serviços, gerando relatórios analíticos e alertas automáticos para avaliações negativas. Toda a lógica de negócio está encapsulada no módulo `core`, sem dependência de frameworks externos.

Este projeto é de nível didático, então todas as escolhas de tecnologia seguem priorizando planos gratuítos.

---

## Entidades e Objetos de Valor

### `Feedback` — Entidade Principal

Representa a avaliação enviada por um usuário.

| Campo       | Tipo   | Descrição                     |
|-------------|--------|-------------------------------|
| `descricao` | String | Texto descritivo da avaliação |
| `nota`      | Long   | Pontuação numérica de 1 a 5   |

#### Regras de Validação

| Campo       | Regra                                          | Mensagem de Erro                |
|-------------|------------------------------------------------|---------------------------------|
| `descricao` | Não pode ser nula nem vazia                    | `"Descrição é obrigatória"`     |
| `nota`      | Não pode ser nula                              | `"Nota é obrigatória"`          |
| `nota`      | Deve estar no intervalo fechado $[1, 5]$       | `"Nota deve ser entre 1 e 5"`   |

A validação é centralizada em `Feedback.validar()`, que retorna `null` quando o feedback é válido ou uma lista de strings de erro quando há violações. **Esta é a única fonte de verdade para integridade dos dados.**

#### Classificação de Feedback

| Método               | Condição          | Significado                         |
|----------------------|-------------------|-------------------------------------|
| `ehItemSatisfatorio()` | nota $\ge 4$     | Avaliação positiva do usuário       |
| `ehItemCritico()`    | nota $< 3$        | Avaliação negativa, exige atenção   |

> Nota 3 não é nem crítica nem satisfatória — representa neutralidade.

---

### `FeedbackPostado` — Evento de Domínio (Record)

Representa o evento gerado imediatamente após a postagem bem-sucedida de um feedback. É publicado na fila de mensageria para consumo assíncrono.

| Campo           | Tipo    | Descrição                                              |
|-----------------|---------|--------------------------------------------------------|
| `descricao`     | String  | Texto da avaliação                                     |
| `nota`          | Long    | Nota atribuída                                         |
| `ehItemCritico` | Boolean | Indica se a nota é menor que 3 (critério de criticidade) |

---

### `RelatorioPeriodico` — Objeto de Valor Calculado

Encapsula os cálculos estatísticos de um conjunto de feedbacks de um período. **Todos os cálculos ocorrem no momento da instanciação** (construtor).

| Atributo                 | Fórmula                                                   |
|--------------------------|-----------------------------------------------------------|
| `totalAvaliacoes`        | Contagem total de feedbacks no período                    |
| `mediaAvaliacoes`        | $\frac{\sum_{i=1}^{n} nota_i}{n}$ (divisão inteira)      |
| `porcentagemDeSatisfeitos` | $\frac{\text{total com nota} \ge 4}{n} \times 100$ (divisão inteira) |

> **Atenção:** Os cálculos utilizam divisão inteira (`Long`). Listas vazias causarão `ArithmeticException` (divisão por zero). A camada de entrada é responsável por garantir que o período retorne ao menos um feedback antes de instanciar o relatório.

---

## Casos de Uso

### `PostarFeedback` — Registrar uma Avaliação

**Entrada:** `descricao`, `nota`, porta `IPublicadorEventos`, porta `IRepositorioFeedback`

**Fluxo:**
```
1. Criar instância de Feedback com descricao e nota
2. Executar Feedback.validar()
   └─ Se houver erros → retornar lista de erros (sem persistir)
3. Persistir o feedback via IRepositorioFeedback.salvar()
4. Publicar FeedbackPostado via IPublicadorEventos.publicar()
5. Retornar null (indicativo de sucesso)
```

**Regra:** A persistência só ocorre após validação bem-sucedida. O evento só é publicado após a persistência bem-sucedida. Em caso de falha em qualquer etapa de infraestrutura, o erro é propagado para a borda.

---

### `GerarRelatorioPeriodico` — Consultar Estatísticas

**Entrada:** `dataInicial`, `dataFinal`, porta `IRepositorioFeedback`

**Fluxo:**
```
1. Buscar feedbacks no período via IRepositorioFeedback.obterFeedbacks(dataInicial, dataFinal)
2. Instanciar RelatorioPeriodico com a lista retornada
3. Retornar o RelatorioPeriodico calculado
```

**Regra:** O intervalo de datas é inclusivo em ambas as extremidades. O filtro é aplicado sobre o campo `criadoEm` dos registros persistidos.

---

### `NotificarItemCritico` — Alertar sobre Avaliação Negativa

**Entrada:** `mensagem` (String com o conteúdo do evento), porta `INotificadorEmail`

**Fluxo:**
```
1. Delegar o envio de notificação para INotificadorEmail.notificarItemCritico(mensagem)
```

**Regra:** Este caso de uso é acionado de forma assíncrona, após o consumo de um evento da fila. A responsabilidade de interpretar o evento (`FeedbackPostado`) e decidir se a notificação deve ser enviada é do adaptador de entrada (`lambda-notificar-critico`).

---

### `ValidadorAutenticacao` — Controle de Acesso por API Key

**Entrada:** `apiKey` (String)

**Fluxo:**
```
1. Se apiKey for nula ou em branco → retornar false
2. Calcular hash SHA-256 da apiKey informada
3. Comparar com o hash esperado armazenado na classe
4. Retornar true se os hashes forem iguais, false caso contrário
```

**Regra de Segurança:** A chave de acesso nunca é armazenada em texto simples. Apenas o hash SHA-256 fica no código. Para trocar a chave: `echo -n "nova-chave" | sha256sum` e atualizar a constante `HASH_ESPERADO`.

---

## Portas (Contratos com a Infraestrutura)

O `core` define **o que** precisa, sem saber **como** será implementado.

| Interface              | Método(s)                                         | Implementação           |
|------------------------|---------------------------------------------------|-------------------------|
| `IRepositorioFeedback` | `salvar(Feedback)`, `obterFeedbacks(Date, Date)`  | `AdaptadorRepositorioDynamoDB` |
| `IPublicadorEventos`   | `publicar(Object)`                               | `AdaptadorPublicadorSqs`       |
| `INotificadorEmail`    | `notificarItemCritico(String)`                   | `AdaptadorNotificadorEmail`    |

---

## Fluxo Completo do Sistema

```
Usuário / API Gateway
        │
        ▼
┌─────────────────────────┐
│  lambda-postar-feedback  │  ← Valida apiKey via ValidadorAutenticacao
│  (Entry Point)           │
└────────────┬────────────┘
             │ PostarFeedback.executar(...)
             ├──────────────────────────────────► DynamoDB (salvar)
             └──────────────────────────────────► SQS (publicar FeedbackPostado)
                                                        │
                                                        │ (assíncrono)
                                                        ▼
                                          ┌──────────────────────────────┐
                                          │  lambda-notificar-critico     │
                                          │  Consome SQS, verifica        │
                                          │  ehItemCritico == true        │
                                          │  → NotificarItemCritico.exec. │
                                          │  → Envia e-mail via SMTP      │
                                          └──────────────────────────────┘

Usuário / API Gateway
        │
        ▼
┌──────────────────────────┐
│  lambda-gerar-relatorio   │  ← Valida apiKey via ValidadorAutenticacao
│  (Entry Point)            │
└─────────────┬────────────┘
              │ GerarRelatorioPeriodico.executar(...)
              └──────────────────────────────────► DynamoDB (obterFeedbacks)
                                                   → Retorna RelatorioPeriodico
```
