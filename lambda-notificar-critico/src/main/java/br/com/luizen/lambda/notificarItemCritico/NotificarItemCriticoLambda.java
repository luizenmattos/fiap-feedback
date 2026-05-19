package br.com.luizen.lambda.notificarItemCritico;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

import org.jboss.logging.Logger;

import br.com.luizen.core.NotificarItemCritico;
import br.com.luizen.core.ports.IConsumidorEventos;
import jakarta.inject.Named;
import jakarta.inject.Inject;

@Named("notificarItemCritico")
public class NotificarItemCriticoLambda implements RequestHandler<Object, String> {

    private static final Logger LOG = Logger.getLogger(NotificarItemCriticoLambda.class);

    @Inject
    IConsumidorEventos consumidorEventos;

    @Override
    public String handleRequest(Object input, Context context) {
        LOG.info("Iniciando processamento de item crítico.");
        String mensagem = consumidorEventos.consumir();

        if (mensagem != null) {
            LOG.info("Mensagem crítica recebida. Executando notificação.");
            NotificarItemCritico.executar(mensagem);
            LOG.info("Notificação de item crítico processada com sucesso.");
            return "Notificação enviada com sucesso";
        }

        LOG.info("Nenhuma mensagem crítica disponível para processar.");
        return "Nenhuma mensagem para processar";
    }
}
