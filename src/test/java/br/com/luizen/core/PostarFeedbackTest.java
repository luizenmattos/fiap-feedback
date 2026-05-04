package br.com.luizen.core;

import org.junit.jupiter.api.Test;

import java.util.List;

import br.com.luizen.core.ports.IPublicadorEventos;

import static org.junit.jupiter.api.Assertions.*;

class PostarFeedbackTest {

    static class DummyPublicador implements IPublicadorEventos {
        int count = 0;
        @Override
        public void publicar(Object evento) {
            count++;
        }
    }

    @Test
    void testExecutarValido() {
        DummyPublicador publicador = new DummyPublicador();
        List<String> erros = PostarFeedback.executar("Muito bom", 5L, publicador);
        assertNull(erros);
        assertEquals(1, publicador.count);
    }

    @Test
    void testExecutarInvalido() {
        DummyPublicador publicador = new DummyPublicador();
        List<String> erros = PostarFeedback.executar("", null, publicador);
        assertNotNull(erros);
        assertEquals(2, erros.size());
        assertTrue(erros.contains("Descrição é obrigatória"));
        assertTrue(erros.contains("Nota é obrigatória"));
        assertEquals(0, publicador.count);
    }

    @Test
    void testExecutarItemCritico() {
        DummyPublicador publicador = new DummyPublicador();
        List<String> erros = PostarFeedback.executar("Muito ruim", 2L, publicador);
        assertNull(erros);
        assertEquals(1, publicador.count);
    }
}
