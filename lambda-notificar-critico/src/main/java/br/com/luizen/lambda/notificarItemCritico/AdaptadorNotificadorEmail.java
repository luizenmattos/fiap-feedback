package br.com.luizen.lambda.notificarItemCritico;

import org.jboss.logging.Logger;

import br.com.luizen.core.ports.INotificadorEmail;
import io.quarkus.mailer.Mail;
import io.quarkus.mailer.Mailer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class AdaptadorNotificadorEmail implements INotificadorEmail {

    private static final Logger LOG = Logger.getLogger(AdaptadorNotificadorEmail.class);

    private static final String DESTINATARIO = "equipe@empresa.com";
    private static final String ASSUNTO = "Feedback crítico recebido";

    @Inject
    Mailer mailer;

    @Override
    public void notificarItemCritico(String mensagem) {
        LOG.infof("Enviando notificação de item crítico por email. destinatario=%s", DESTINATARIO);
        try {
            mailer.send(
                Mail.withText(DESTINATARIO, ASSUNTO, montarCorpo(mensagem))
            );
            LOG.infof("Email de item crítico enviado com sucesso. destinatario=%s", DESTINATARIO);
        } catch (Exception e) {
            LOG.errorf(e, "Falha ao enviar email de item crítico. destinatario=%s", DESTINATARIO);
            throw new RuntimeException("Falha ao enviar email de notificação: " + e.getMessage(), e);
        }
    }

    private String montarCorpo(String mensagem) {
        return """
                Um feedback com nota crítica foi registrado no sistema.

                Detalhes:
                %s

                ---
                Sistema fiap-feedback
                """.formatted(mensagem);
    }
}
