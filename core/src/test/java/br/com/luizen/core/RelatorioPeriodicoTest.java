package br.com.luizen.core;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RelatorioPeriodicoTest {

    @Test
    void deveCalcularTotalAvaliacoes() {
        List<Feedback> feedbacks = List.of(
            Feedback.criar("Ótimo", 5L),
            Feedback.criar("Bom", 4L),
            Feedback.criar("Regular", 2L)
        );

        RelatorioPeriodico relatorio = new RelatorioPeriodico(feedbacks);

        assertEquals(3L, relatorio.totalAvaliacoes);
    }

    @Test
    void deveCalcularMediaAvaliacoes() {
        List<Feedback> feedbacks = List.of(
            Feedback.criar("Ótimo", 5L),
            Feedback.criar("Bom", 4L),
            Feedback.criar("Regular", 3L)
        );

        RelatorioPeriodico relatorio = new RelatorioPeriodico(feedbacks);

        assertEquals(4L, relatorio.mediaAvaliacoes);
    }

    @Test
    void deveCalcularPorcentagemDeSatisfeitos() {
        List<Feedback> feedbacks = List.of(
            Feedback.criar("Ótimo", 5L),
            Feedback.criar("Bom", 4L),
            Feedback.criar("Ruim", 1L),
            Feedback.criar("Péssimo", 2L)
        );

        RelatorioPeriodico relatorio = new RelatorioPeriodico(feedbacks);

        // 2 satisfeitos de 4 = 50%
        assertEquals(50L, relatorio.porcentagemDeSatisfeitos);
    }

    @Test
    void deveTerPorcentagemCemQuandoTodosSatisfeitos() {
        List<Feedback> feedbacks = List.of(
            Feedback.criar("Ótimo", 5L),
            Feedback.criar("Bom", 4L)
        );

        RelatorioPeriodico relatorio = new RelatorioPeriodico(feedbacks);

        assertEquals(100L, relatorio.porcentagemDeSatisfeitos);
    }

    @Test
    void deveTerPorcentagemZeroQuandoNenhumSatisfeito() {
        List<Feedback> feedbacks = List.of(
            Feedback.criar("Ruim", 1L),
            Feedback.criar("Péssimo", 2L),  
            Feedback.criar("Péssimo", 3L)
        );

        RelatorioPeriodico relatorio = new RelatorioPeriodico(feedbacks);

        assertEquals(0L, relatorio.porcentagemDeSatisfeitos);
    }

    @Test
    void deveCalcularMediaComDivisaoInteira() {
        List<Feedback> feedbacks = List.of(
            Feedback.criar("Ótimo", 5L),
            Feedback.criar("Ruim", 2L)
        );

        RelatorioPeriodico relatorio = new RelatorioPeriodico(feedbacks);

        // (5 + 2) / 2 = 3 (divisão inteira)
        assertEquals(3L, relatorio.mediaAvaliacoes);
    }
}
