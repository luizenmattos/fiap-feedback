package br.com.luizen.lambda.gerarRelatorio;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

import org.jboss.logging.Logger;

import br.com.luizen.core.GerarRelatorioPeriodico;
import br.com.luizen.core.RelatorioPeriodico;
import br.com.luizen.core.ValidadorAutenticacao;
import br.com.luizen.core.ports.IRepositorioFeedback;
import io.quarkus.mailer.Mail;
import io.quarkus.mailer.Mailer;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@Path("/relatorio")
public class GerarRelatorioResource {

    private static final Logger LOG = Logger.getLogger(GerarRelatorioResource.class);

    @Inject
    IRepositorioFeedback repositorioFeedback;

    @Inject
    Mailer mailer;

    @ConfigProperty(name = "email.destinatario")
    String destinatario;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response gerar(@QueryParam("apiKey") String apiKey) {
        LocalDateTime now = LocalDateTime.now(ZoneId.of("America/Sao_Paulo"));
        LocalDateTime dataFinalLdt = now.withHour(23).withMinute(59).withSecond(59).withNano(0);
        LocalDateTime dataInicialLdt = dataFinalLdt.minusDays(7);
        Date dataInicial = Date.from(dataInicialLdt.atZone(ZoneId.systemDefault()).toInstant());
        Date dataFinal = Date.from(dataFinalLdt.atZone(ZoneId.systemDefault()).toInstant());

        LOG.infof("Iniciando geração de relatório semanal. periodo=%s a %s", dataInicial, dataFinal);

        if (!ValidadorAutenticacao.validar(apiKey)) {
            LOG.warn("Tentativa de acesso não autorizado em gerar-relatorio.");
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(RelatorioOutput.naoAutorizado())
                    .build();
        }

        try {
            RelatorioPeriodico relatorio = GerarRelatorioPeriodico.executar(dataInicial, dataFinal, repositorioFeedback);

            LOG.infof("Relatório gerado com sucesso. total=%d, media=%d, satisfeitos=%d%%",
                    relatorio.getTotalAvaliacoes(),
                    relatorio.getMediaAvaliacoes(),
                    relatorio.getPorcentagemDeSatisfeitos());

            enviarRelatorioSemanal(relatorio);

            return Response.ok(RelatorioOutput.relatorioGerado(
                    relatorio.getTotalAvaliacoes(),
                    relatorio.getMediaAvaliacoes(),
                    relatorio.getPorcentagemDeSatisfeitos()
            )).build();
        } catch (Exception e) {
            LOG.errorf(e, "Falha ao gerar relatório semanal. periodo=%s a %s", dataInicial, dataFinal);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(RelatorioOutput.relatorioComErro("Erro ao gerar relatório: " + e.getMessage()))
                    .build();
        }
    }

    //TODO: extrair envio de relatorio para outra classe lambda; criar um infra-email para usar em gerar-relatorio e notificar-item-critico
    private void enviarRelatorioSemanal(RelatorioPeriodico relatorio) {
        String assunto = "Relatório semanal de feedbacks";
        String corpo = montarCorpo(relatorio);
        try {
            mailer.send(Mail.withText(destinatario, assunto, corpo));
            LOG.infof("Email de relatório semanal enviado com sucesso. destinatario=%s", destinatario);
        } catch (Exception e) {
            LOG.errorf(e, "Falha ao enviar email de relatório semanal. destinatario=%s", destinatario);
            throw new RuntimeException("Falha ao enviar email de relatório semanal: " + e.getMessage(), e);
        }
    }

    private String montarCorpo(RelatorioPeriodico relatorio) {
        return String.format("""
                Relatório semanal de feedbacks:

                Total de avaliações: %d
                Média das avaliações: %d
                Porcentagem de satisfeitos: %d%%

                ---\nSistema fiap-feedback
                """,
                relatorio.getTotalAvaliacoes(),
                relatorio.getMediaAvaliacoes(),
                relatorio.getPorcentagemDeSatisfeitos());
    }
}
