package br.com.luizen.core;

import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.List;

import br.com.luizen.core.ports.IRepositorioFeedback;

import static org.junit.jupiter.api.Assertions.*;

class GerarRelatorioPeriodicoTest {

    static class RepositorioFake implements IRepositorioFeedback {
        private final List<Feedback> feedbacksRetornados;
        Date dataInicialRecebida;
        Date dataFinalRecebida;

        RepositorioFake(List<Feedback> feedbacksRetornados) {
            this.feedbacksRetornados = feedbacksRetornados;
        }

        @Override
        public void salvar(Feedback feedback) {}

        @Override
        public List<Feedback> obterFeedbacks(Date dataInicial, Date dataFinal) {
            this.dataInicialRecebida = dataInicial;
            this.dataFinalRecebida = dataFinal;
            return feedbacksRetornados;
        }
    }

    @Test
    void deveRetornarRelatorioComFeedbacksDoRepositorio() {
        List<Feedback> feedbacks = List.of(
            Feedback.criar("Ótimo", 5L),
            Feedback.criar("Bom", 4L),
            Feedback.criar("Ruim", 1L)
        );
        RepositorioFake repositorio = new RepositorioFake(feedbacks);
        Date dataInicial = new Date(0);
        Date dataFinal = new Date();

        RelatorioPeriodico relatorio = GerarRelatorioPeriodico.executar(dataInicial, dataFinal, repositorio);

        assertNotNull(relatorio);
        assertEquals(3L, relatorio.totalAvaliacoes);
    }

    @Test
    void deveRepassarDatasCorretasAoRepositorio() {
        RepositorioFake repositorio = new RepositorioFake(List.of(
            Feedback.criar("Ok", 3L)
        ));
        Date dataInicial = new Date(1000L);
        Date dataFinal = new Date(9000L);

        GerarRelatorioPeriodico.executar(dataInicial, dataFinal, repositorio);

        assertEquals(dataInicial, repositorio.dataInicialRecebida);
        assertEquals(dataFinal, repositorio.dataFinalRecebida);
    }

    @Test
    void deveCalcularMediaCorretamenteComFeedbacksDoRepositorio() {
        List<Feedback> feedbacks = List.of(
            Feedback.criar("Ótimo", 5L),
            Feedback.criar("Bom", 3L)
        );
        RepositorioFake repositorio = new RepositorioFake(feedbacks);

        RelatorioPeriodico relatorio = GerarRelatorioPeriodico.executar(new Date(0), new Date(), repositorio);

        // (5 + 3) / 2 = 4
        assertEquals(4L, relatorio.mediaAvaliacoes);
    }

    @Test
    void deveCalcularPorcentagemDeSatisfeitosComFeedbacksDoRepositorio() {
        List<Feedback> feedbacks = List.of(
            Feedback.criar("Ótimo", 5L),
            Feedback.criar("Ruim", 2L),
            Feedback.criar("Péssimo", 1L),
            Feedback.criar("Bom", 4L)
        );
        RepositorioFake repositorio = new RepositorioFake(feedbacks);

        RelatorioPeriodico relatorio = GerarRelatorioPeriodico.executar(new Date(0), new Date(), repositorio);

        // 2 satisfeitos de 4 = 50%
        assertEquals(50L, relatorio.porcentagemDeSatisfeitos);
    }
}
