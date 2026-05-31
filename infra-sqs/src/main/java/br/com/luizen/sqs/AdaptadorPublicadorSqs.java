package br.com.luizen.sqs;

import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import jakarta.enterprise.context.ApplicationScoped;

import br.com.luizen.core.FeedbackPostado;
import br.com.luizen.core.ports.IPublicadorEventos;

@ApplicationScoped
public class AdaptadorPublicadorSqs implements IPublicadorEventos {

    private static final Logger LOG = Logger.getLogger(AdaptadorPublicadorSqs.class);

    private final SqsClient sqsClient;
    private final String urlFila;
    private final ObjectMapper mapper;

    public AdaptadorPublicadorSqs(@ConfigProperty(name = "sqs.queue.url") String urlFila,
                                   @ConfigProperty(name = "aws.region") String awsRegion) {
        this.sqsClient = SqsClient.builder()
                .region(Region.of(awsRegion))
                .httpClientBuilder(UrlConnectionHttpClient.builder())
                .build();
        this.urlFila = urlFila;
        this.mapper = new ObjectMapper();
    }

    @Override
    public void publicar(Object evento) {
        LOG.debugf("Publicando evento no SQS. tipo=%s", evento.getClass().getSimpleName());
        try {
            if (evento instanceof FeedbackPostado) {
                String mensagemJson = mapper.writeValueAsString(evento);

                SendMessageRequest request = SendMessageRequest.builder()
                        .queueUrl(urlFila)
                        .messageBody(mensagemJson)
                        .build();

                sqsClient.sendMessage(request);
                LOG.info("Evento publicado no SQS com sucesso.");
            } else {
                LOG.warnf("Tipo de evento não suportado e ignorado. tipo=%s", evento.getClass().getSimpleName());
            }
        } catch (Exception e) {
            LOG.errorf(e, "Falha ao publicar evento no SQS.");
            throw new RuntimeException("Falha ao publicar evento no SQS: " + e.getMessage(), e);
        }
    }

}
