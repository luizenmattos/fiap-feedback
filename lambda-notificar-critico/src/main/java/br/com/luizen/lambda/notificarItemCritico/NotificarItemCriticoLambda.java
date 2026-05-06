package br.com.luizen.lambda.notificarItemCritico;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

import br.com.luizen.core.NotificarItemCritico;
import br.com.luizen.core.ports.IConsumidorEventos;
import jakarta.inject.Named;
import jakarta.inject.Inject;

@Named("notificarItemCritico")
public class NotificarItemCriticoLambda implements RequestHandler<Object, String> {

    @Inject
    IConsumidorEventos consumidorEventos;

    @Override
    public String handleRequest(Object input, Context context) {
        String mensagem = consumidorEventos.consumir();

        if (mensagem != null) {
            NotificarItemCritico.executar(mensagem);
            return "Notificação enviada com sucesso";
        }

        return "Nenhuma mensagem para processar";
    }
}
