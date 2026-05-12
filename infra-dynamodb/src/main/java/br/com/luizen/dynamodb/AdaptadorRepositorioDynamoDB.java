package br.com.luizen.dynamodb;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import br.com.luizen.core.Feedback;
import br.com.luizen.core.ports.IRepositorioFeedback;
import jakarta.enterprise.context.ApplicationScoped;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanResponse;

@ApplicationScoped
public class AdaptadorRepositorioDynamoDB implements IRepositorioFeedback {

    private final DynamoDbClient dynamoDbClient;
    private final String nomeTabela;

    public AdaptadorRepositorioDynamoDB(@ConfigProperty(name = "dynamodb.tabela.feedbacks", defaultValue = "feedbacks") String nomeTabela) {
        this.dynamoDbClient = DynamoDbClient.builder()
                .region(Region.US_EAST_2)
                .httpClientBuilder(UrlConnectionHttpClient.builder())
                .build();
        this.nomeTabela = nomeTabela;
    }

    @Override
    public void salvar(Feedback feedback) {
        Map<String, AttributeValue> item = Map.of(
                "id", AttributeValue.fromS(UUID.randomUUID().toString()),
                "descricao", AttributeValue.fromS(feedback.getDescricao()),
                "nota", AttributeValue.fromN(feedback.getNota().toString()),
                "ehItemCritico", AttributeValue.fromBool(feedback.ehItemCritico()),
                "criadoEm", AttributeValue.fromS(Instant.now().toString())
        );

        PutItemRequest requisicao = PutItemRequest.builder()
                .tableName(nomeTabela)
                .item(item)
                .build();

        try {
            dynamoDbClient.putItem(requisicao);
        } catch (Exception e) {
            throw new RuntimeException("Falha ao salvar feedback no DynamoDB: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Feedback> obterFeedbacks(Date dataInicial, Date dataFinal) {
        String inicio = dataInicial.toInstant().toString();
        String fim = dataFinal.toInstant().toString();

        ScanRequest requisicao = ScanRequest.builder()
                .tableName(nomeTabela)
                .filterExpression("criadoEm >= :inicio AND criadoEm <= :fim")
                .expressionAttributeValues(Map.of(
                        ":inicio", AttributeValue.fromS(inicio),
                        ":fim", AttributeValue.fromS(fim)
                ))
                .build();

        try {
            ScanResponse resposta = dynamoDbClient.scan(requisicao);
            return resposta.items().stream()
                    .map(item -> Feedback.criar(
                            item.get("descricao").s(),
                            Long.parseLong(item.get("nota").n())
                    ))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new RuntimeException("Falha ao obter feedbacks do DynamoDB: " + e.getMessage(), e);
        }
    }
}
