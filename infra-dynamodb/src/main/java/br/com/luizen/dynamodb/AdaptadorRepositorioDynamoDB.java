package br.com.luizen.dynamodb;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import br.com.luizen.core.Feedback;
import br.com.luizen.core.ports.IRepositorioFeedback;
import jakarta.enterprise.context.ApplicationScoped;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

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
}
