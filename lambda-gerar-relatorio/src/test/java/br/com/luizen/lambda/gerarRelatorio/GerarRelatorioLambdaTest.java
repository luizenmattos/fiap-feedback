package br.com.luizen.lambda.gerarRelatorio;

import org.junit.jupiter.api.Test;

import br.com.luizen.core.Feedback;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GerarRelatorioLambdaTest {

    @Test
    void deveGerarRelatorioComSucesso() {
        GerarRelatorioLambda lambda = new GerarRelatorioLambda();
        lambda.repositorioFeedback = new MockRepositorioFeedback(List.of(
                Feedback.criar("Ótimo", 5L),
                Feedback.criar("Bom", 4L),
                Feedback.criar("Ruim", 1L)
        ));

        RelatorioInput input = new RelatorioInput();
        input.dataInicial = "2026-01-01T00:00:00Z";
        input.dataFinal = "2026-05-01T00:00:00Z";

        RelatorioOutput output = lambda.handleRequest(input, null);

        assertNotNull(output);
        assertEquals("Relatório gerado com sucesso", output.getMensagem());
        assertEquals(3L, output.getTotalAvaliacoes());
        assertEquals(3L, output.getMediaAvaliacoes()); // (5+4+1)/3 = 3
        // 2 satisfeitos de 3 = 66%
        assertEquals(66L, output.getPorcentagemDeSatisfeitos());
    }

    @Test
    void deveRetornarErroComDataInvalida() {
        GerarRelatorioLambda lambda = new GerarRelatorioLambda();
        lambda.repositorioFeedback = new MockRepositorioFeedback(List.of());

        RelatorioInput input = new RelatorioInput();
        input.dataInicial = "data-invalida";
        input.dataFinal = "2026-05-01T00:00:00Z";

        RelatorioOutput output = lambda.handleRequest(input, null);

        assertNotNull(output);
        assertNotNull(output.getMensagem());
        assertTrue(output.getMensagem().startsWith("Erro ao gerar relatório:"));
    }

    @Test
    void deveGerarRelatorioComTodosSatisfeitos() {
        GerarRelatorioLambda lambda = new GerarRelatorioLambda();
        lambda.repositorioFeedback = new MockRepositorioFeedback(List.of(
                Feedback.criar("Excelente", 5L),
                Feedback.criar("Bom", 4L),
                Feedback.criar("Ok", 3L)
        ));

        RelatorioInput input = new RelatorioInput();
        input.dataInicial = "2026-01-01T00:00:00Z";
        input.dataFinal = "2026-05-01T00:00:00Z";

        RelatorioOutput output = lambda.handleRequest(input, null);

        assertEquals(100L, output.getPorcentagemDeSatisfeitos());
        assertEquals(3L, output.getTotalAvaliacoes());
    }
}
