package br.com.luizen.lambda.postarFeedback;

import org.junit.jupiter.api.Test;

import com.amazonaws.services.lambda.runtime.Context;

import static org.junit.jupiter.api.Assertions.*;

class PostarFeedbackLambdaTest {

    private static final String apiKeyValida = "7c3f9371e1327c96ff069135205a0b669943c145bcc9833cc3caa9d5847c5c8d";

    @Test
    void testHandleRequestSucesso() {
        PostarFeedbackLambda lambda = new PostarFeedbackLambda();
        lambda.publicadorEventos = new MockPublicadorEventos();
        lambda.repositorioFeedback = new MockRepositorioFeedback();
        FeedbackInput input = new FeedbackInput();
        input.descricao = "Ótimo produto";
        input.nota = 5L;
        input.apiKey = apiKeyValida;

        Context context = null;

        FeedbackOutput output = lambda.handleRequest(input, context);

        assertNotNull(output);
        assertEquals("Feedback recebido com nota: 5", output.getMensagem());
        assertNull(output.getErros());
    }

    @Test
    void testHandleRequestErroValidacao() {
        PostarFeedbackLambda lambda = new PostarFeedbackLambda();
        lambda.publicadorEventos = new MockPublicadorEventos();
        lambda.repositorioFeedback = new MockRepositorioFeedback();
        FeedbackInput input = new FeedbackInput();
        input.descricao = "";
        input.nota = 6L;
        input.apiKey = apiKeyValida;

        Context context = null;

        FeedbackOutput output = lambda.handleRequest(input, context);

        assertNotNull(output);
        assertEquals("Erro ao postar feedback", output.getMensagem());
        assertNotNull(output.getErros());
        assertTrue(output.getErros().contains("Descrição é obrigatória"));
        assertTrue(output.getErros().contains("Nota deve ser entre 1 e 5"));
    }
}
