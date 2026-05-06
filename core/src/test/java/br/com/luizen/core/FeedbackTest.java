package br.com.luizen.core;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FeedbackTest {

    @Test
    void testCriarFeedbackValido() {
        Feedback feedback = Feedback.criar("Ótimo serviço", 5L);
        assertNotNull(feedback);
        assertEquals("Ótimo serviço", feedback.descricao);
        assertEquals(5L, feedback.nota);
    }

    @Test
    void testEhItemCritico() {
        Feedback feedbackCritico = Feedback.criar("Péssimo", 2L);
        assertTrue(feedbackCritico.ehItemCritico());

        Feedback feedbackNeutro = Feedback.criar("Mais ou menos", 3L);
        assertFalse(feedbackNeutro.ehItemCritico());

        Feedback feedbackSatisfeito = Feedback.criar("Bom", 4L);
        assertFalse(feedbackSatisfeito.ehItemCritico());
    }

    @Test
    void testValidarValido() {
        Feedback feedback = Feedback.criar("Excelente", 5L);
        assertNull(feedback.validar());
    }

    @Test
    void testValidarSemDescricao() {
        Feedback feedback = Feedback.criar(null, 5L);
        List<String> erros = feedback.validar();
        assertNotNull(erros);
        assertTrue(erros.contains("Descrição é obrigatória"));
    }

    @Test
    void testValidarSemNota() {
        Feedback feedback = Feedback.criar("Descrição", null);
        List<String> erros = feedback.validar();
        assertNotNull(erros);
        assertTrue(erros.contains("Nota é obrigatória"));
    }

    @Test
    void testValidarNotaInvalida() {
        Feedback feedbackAcima = Feedback.criar("Bom", 6L);
        assertTrue(feedbackAcima.validar().contains("Nota deve ser entre 1 e 5"));

        Feedback feedbackAbaixo = Feedback.criar("Ruim", 0L);
        assertTrue(feedbackAbaixo.validar().contains("Nota deve ser entre 1 e 5"));
    }
}
