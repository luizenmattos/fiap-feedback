package br.com.luizen.lambda.gerarRelatorio;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import br.com.luizen.core.Feedback;
import io.quarkus.mailer.Mailer;
import io.quarkus.mailer.Mail;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GerarRelatorioLambdaTest {

    private static final String apiKeyValida = "7c3f9371e1327c96ff069135205a0b669943c145bcc9833cc3caa9d5847c5c8d";

    @Test
    void deveGerarRelatorioComSucesso() {
        GerarRelatorioLambda lambda = new GerarRelatorioLambda();
        lambda.repositorioFeedback = new MockRepositorioFeedback(List.of(
                Feedback.criar("Ótimo", 5L),
                Feedback.criar("Bom", 4L),
                Feedback.criar("Ruim", 1L)
        ));
        lambda.mailer = Mockito.mock(Mailer.class);

        RelatorioInput input = new RelatorioInput();
        input.apiKey = apiKeyValida;
        RelatorioOutput output = lambda.handleRequest(input, null);

        assertNotNull(output);
        assertEquals("Relatório gerado com sucesso", output.getMensagem());
        assertEquals(3L, output.getTotalAvaliacoes());
        assertEquals(3L, output.getMediaAvaliacoes());
        assertEquals(66L, output.getPorcentagemDeSatisfeitos());

        // verify DEPOIS do handleRequest
        Mockito.verify(lambda.mailer).send(Mockito.any(Mail.class));
    }

    @Test
    void deveGerarRelatorioComTodosSatisfeitos() {
        GerarRelatorioLambda lambda = new GerarRelatorioLambda();
        lambda.repositorioFeedback = new MockRepositorioFeedback(List.of(
            Feedback.criar("Excelente", 5L),
            Feedback.criar("Bom", 4L)
        ));
        lambda.mailer = Mockito.mock(Mailer.class);

        RelatorioInput input = new RelatorioInput();
        input.apiKey = apiKeyValida;
        RelatorioOutput output = lambda.handleRequest(input, null);

        assertEquals(100L, output.getPorcentagemDeSatisfeitos());
        assertEquals(2L, output.getTotalAvaliacoes());
}
}
