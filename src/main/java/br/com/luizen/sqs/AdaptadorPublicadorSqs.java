package br.com.luizen.sqs;

import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import com.fasterxml.jackson.databind.ObjectMapper; // Exemplo de serializador
import org.eclipse.microprofile.config.inject.ConfigProperty;
import jakarta.enterprise.context.ApplicationScoped;

import br.com.luizen.core.FeedbackPostado;
import br.com.luizen.core.ports.IPublicadorEventos;

@ApplicationScoped
public class AdaptadorPublicadorSqs implements IPublicadorEventos {

    private final SqsClient sqsClient;
    private final String urlFila;
    private final ObjectMapper mapper;

    public AdaptadorPublicadorSqs(@ConfigProperty(name = "sqs.queue.url") String urlFila) {
        this.sqsClient = SqsClient.builder()
                .region(Region.US_EAST_2)
                .httpClientBuilder(UrlConnectionHttpClient.builder())
                .build();
        this.urlFila = urlFila;
        this.mapper = new ObjectMapper();
    }

    @Override
    public void publicar(Object evento) {
        try {
            if (evento instanceof FeedbackPostado) {
                String mensagemJson = mapper.writeValueAsString(evento);
                
                SendMessageRequest request = SendMessageRequest.builder()
                        .queueUrl(urlFila)
                        .messageBody(mensagemJson)
                        .build();

                sqsClient.sendMessage(request);
            }
        } catch (Exception e) {
            // Tratar falha de infraestrutura
            throw new RuntimeException("Falha ao publicar evento no SQS: " + e.getMessage(), e);
        }
    }
}