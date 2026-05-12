package br.com.luizen.lambda.gerarRelatorio;

import java.time.Instant;
import java.util.Date;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

import br.com.luizen.core.GerarRelatorioPeriodico;
import br.com.luizen.core.RelatorioPeriodico;
import br.com.luizen.core.ports.IRepositorioFeedback;
import jakarta.inject.Inject;
import jakarta.inject.Named;

@Named("gerarRelatorio")
public class GerarRelatorioLambda implements RequestHandler<RelatorioInput, RelatorioOutput> {

    @Inject
    IRepositorioFeedback repositorioFeedback;

    @Override
    public RelatorioOutput handleRequest(RelatorioInput input, Context context) {
        try {
            Date dataInicial = Date.from(Instant.parse(input.dataInicial));
            Date dataFinal = Date.from(Instant.parse(input.dataFinal));

            RelatorioPeriodico relatorio = GerarRelatorioPeriodico.executar(dataInicial, dataFinal, repositorioFeedback);

            return RelatorioOutput.relatorioGerado(
                    relatorio.getTotalAvaliacoes(),
                    relatorio.getMediaAvaliacoes(),
                    relatorio.getPorcentagemDeSatisfeitos()
            );
        } catch (Exception e) {
            return RelatorioOutput.relatorioComErro("Erro ao gerar relatório: " + e.getMessage());
        }
    }
}
