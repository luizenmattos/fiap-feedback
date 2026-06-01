package br.com.luizen.lambda.notificarItemCritico;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.jboss.logging.Logger;

import br.com.luizen.core.FeedbackPostado;
import br.com.luizen.core.NotificarItemCritico;
import br.com.luizen.core.ports.INotificadorEmail;
import jakarta.inject.Inject;
import jakarta.inject.Named;

@Named("notificarItemCritico")
public class NotificarItemCriticoLambda implements RequestHandler<SQSEvent, String> {

    private static final Logger LOG = Logger.getLogger(NotificarItemCriticoLambda.class);

    @Inject
    INotificadorEmail notificadorEmail;

    @Inject
    ObjectMapper objectMapper;

    @Override
    public String handleRequest(SQSEvent input, Context context) {
        LOG.info("Iniciando processamento de notificação de item crítico.");

        for (SQSEvent.SQSMessage msg : input.getRecords()) {
            String corpo = msg.getBody();
            LOG.infof("Mensagem recebida da fila: %s", corpo);

            try {
                FeedbackPostado evento = objectMapper.readValue(corpo, FeedbackPostado.class);

                if (!Boolean.TRUE.equals(evento.ehItemCritico())) {
                    LOG.infof("Feedback ignorado — não é item crítico. nota=%d", evento.nota());
                    continue;
                }

                String mensagemEmail = String.format("Nota: %d — %s", evento.nota(), evento.descricao());
                NotificarItemCritico.executar(mensagemEmail, notificadorEmail);
                LOG.infof("Notificação de item crítico enviada com sucesso. nota=%d", evento.nota());

            } catch (Exception e) {
                LOG.errorf(e, "Erro ao processar mensagem da fila: %s", corpo);
                return "Erro ao processar mensagem: " + e.getMessage();
            }
        }
        return "Processamento concluído.";
    }
}
