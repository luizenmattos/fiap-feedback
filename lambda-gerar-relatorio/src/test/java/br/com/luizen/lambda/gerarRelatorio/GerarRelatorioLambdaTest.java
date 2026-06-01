package br.com.luizen.lambda.gerarRelatorio;

import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import br.com.luizen.core.Feedback;
import io.quarkus.mailer.Mail;
import io.quarkus.mailer.Mailer;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GerarRelatorioLambdaTest {

    private static final String apiKeyValida = "7c3f9371e1327c96ff069135205a0b669943c145bcc9833cc3caa9d5847c5c8d";

    @Test
    void deveGerarRelatorioComSucesso() {
        GerarRelatorioResource resource = new GerarRelatorioResource();
        resource.repositorioFeedback = new MockRepositorioFeedback(List.of(
                Feedback.criar("Ótimo", 5L),
                Feedback.criar("Bom", 4L),
                Feedback.criar("Ruim", 1L)
        ));
        resource.mailer = Mockito.mock(Mailer.class);
        resource.destinatario = "teste@example.com";

        Response response = resource.gerar(apiKeyValida);

        assertEquals(200, response.getStatus());
        RelatorioOutput output = (RelatorioOutput) response.getEntity();
        assertNotNull(output);
        assertEquals("Relatório gerado com sucesso", output.getMensagem());
        assertEquals(3L, output.getTotalAvaliacoes());
        assertEquals(3L, output.getMediaAvaliacoes());
        assertEquals(66L, output.getPorcentagemDeSatisfeitos());

        Mockito.verify(resource.mailer).send(Mockito.any(Mail.class));
    }

    @Test
    void deveGerarRelatorioComTodosSatisfeitos() {
        GerarRelatorioResource resource = new GerarRelatorioResource();
        resource.repositorioFeedback = new MockRepositorioFeedback(List.of(
                Feedback.criar("Excelente", 5L),
                Feedback.criar("Bom", 4L)
        ));
        resource.mailer = Mockito.mock(Mailer.class);
        resource.destinatario = "teste@example.com";

        Response response = resource.gerar(apiKeyValida);

        assertEquals(200, response.getStatus());
        RelatorioOutput output = (RelatorioOutput) response.getEntity();
        assertEquals(100L, output.getPorcentagemDeSatisfeitos());
        assertEquals(2L, output.getTotalAvaliacoes());
    }
}
