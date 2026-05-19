package br.com.luizen.lambda.gerarRelatorio;

import java.time.Instant;
import java.util.Date;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

import org.jboss.logging.Logger;

import br.com.luizen.core.GerarRelatorioPeriodico;
import br.com.luizen.core.RelatorioPeriodico;
import br.com.luizen.core.ValidadorAutenticacao;
import br.com.luizen.core.ports.IRepositorioFeedback;
import jakarta.inject.Inject;
import jakarta.inject.Named;

@Named("gerarRelatorio")
public class GerarRelatorioLambda implements RequestHandler<RelatorioInput, RelatorioOutput> {

    private static final Logger LOG = Logger.getLogger(GerarRelatorioLambda.class);

    @Inject
    IRepositorioFeedback repositorioFeedback;

    @Override
    public RelatorioOutput handleRequest(RelatorioInput input, Context context) {
        LOG.infof("Iniciando geração de relatório. periodo=%s a %s", input.dataInicial, input.dataFinal);

        if (!ValidadorAutenticacao.validar(input.apiKey)) {
            LOG.warn("Tentativa de acesso não autorizado em gerar-relatorio.");
            return RelatorioOutput.naoAutorizado();
        }

        try {
            Date dataInicial = Date.from(Instant.parse(input.dataInicial));
            Date dataFinal = Date.from(Instant.parse(input.dataFinal));

            RelatorioPeriodico relatorio = GerarRelatorioPeriodico.executar(dataInicial, dataFinal, repositorioFeedback);

            LOG.infof("Relatório gerado com sucesso. total=%d, media=%d, satisfeitos=%d%%",
                    relatorio.getTotalAvaliacoes(),
                    relatorio.getMediaAvaliacoes(),
                    relatorio.getPorcentagemDeSatisfeitos());

            return RelatorioOutput.relatorioGerado(
                    relatorio.getTotalAvaliacoes(),
                    relatorio.getMediaAvaliacoes(),
                    relatorio.getPorcentagemDeSatisfeitos()
            );
        } catch (Exception e) {
            LOG.errorf(e, "Falha ao gerar relatório. periodo=%s a %s", input.dataInicial, input.dataFinal);
            return RelatorioOutput.relatorioComErro("Erro ao gerar relatório: " + e.getMessage());
        }
    }
}
