package br.com.luizen.lambda.notificarItemCritico;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;

import org.jboss.logging.Logger;

import br.com.luizen.core.NotificarItemCritico;
import br.com.luizen.core.ports.INotificadorEmail;
import jakarta.inject.Named;
import jakarta.inject.Inject;

@Named("notificarItemCritico")
public class NotificarItemCriticoLambda implements RequestHandler<SQSEvent, String> {

    private static final Logger LOG = Logger.getLogger(NotificarItemCriticoLambda.class);

    @Inject
    INotificadorEmail notificadorEmail;

    @Override
    public String handleRequest(SQSEvent input, Context context) {
        LOG.info("Iniciando processamento de item crítico.");

        for (SQSEvent.SQSMessage msg : input.getRecords()) {
            String mensagem = msg.getBody();
            LOG.infof("Mensagem crítica recebida: %s", mensagem);
            
            NotificarItemCritico.executar(mensagem, notificadorEmail);
            LOG.info("Notificação de item crítico processada com sucesso.");
            
            return "Notificação enviada com sucesso";
        }

        LOG.info("Nenhuma mensagem crítica disponível para processar.");
        return "Nenhuma mensagem para processar";
    }
}
