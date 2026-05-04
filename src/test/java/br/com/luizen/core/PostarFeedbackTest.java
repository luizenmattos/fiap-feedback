package br.com.luizen.core;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PostarFeedbackTest {

    @Test
    void testExecutarValido() {
        List<String> erros = PostarFeedback.executar("Muito bom", 5L);
        assertNull(erros);
    }

    @Test
    void testExecutarInvalido() {
        List<String> erros = PostarFeedback.executar("", null);
        assertNotNull(erros);
        assertEquals(2, erros.size());
        assertTrue(erros.contains("Descrição é obrigatória"));
        assertTrue(erros.contains("Nota é obrigatória"));
    }

    @Test
    void testExecutarItemCritico() {
        List<String> erros = PostarFeedback.executar("Muito ruim", 2L);
        assertNull(erros);
    }
}
