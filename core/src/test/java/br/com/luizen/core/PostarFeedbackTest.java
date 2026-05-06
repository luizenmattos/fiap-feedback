package br.com.luizen.core;

import org.junit.jupiter.api.Test;

import java.util.List;

import br.com.luizen.core.ports.IPublicadorEventos;
import br.com.luizen.core.ports.IRepositorioFeedback;

import static org.junit.jupiter.api.Assertions.*;

class PostarFeedbackTest {

    static class DummyPublicador implements IPublicadorEventos {
        int count = 0;
        @Override
        public void publicar(Object evento) {
            count++;
        }
    }

    static class DummyRepositorio implements IRepositorioFeedback {
        int count = 0;
        @Override
        public void salvar(Feedback feedback) {
            count++;
        }
    }

    @Test
    void testExecutarValido() {
        DummyPublicador publicador = new DummyPublicador();
        DummyRepositorio repositorio = new DummyRepositorio();
        List<String> erros = PostarFeedback.executar("Muito bom", 5L, publicador, repositorio);
        assertNull(erros);
        assertEquals(1, publicador.count);
        assertEquals(1, repositorio.count);
    }

    @Test
    void testExecutarInvalido() {
        DummyPublicador publicador = new DummyPublicador();
        DummyRepositorio repositorio = new DummyRepositorio();
        List<String> erros = PostarFeedback.executar("", null, publicador, repositorio);
        assertNotNull(erros);
        assertEquals(2, erros.size());
        assertTrue(erros.contains("Descrição é obrigatória"));
        assertTrue(erros.contains("Nota é obrigatória"));
        assertEquals(0, publicador.count);
        assertEquals(0, repositorio.count);
    }

    @Test
    void testExecutarItemCritico() {
        DummyPublicador publicador = new DummyPublicador();
        DummyRepositorio repositorio = new DummyRepositorio();
        List<String> erros = PostarFeedback.executar("Muito ruim", 2L, publicador, repositorio);
        assertNull(erros);
        assertEquals(1, publicador.count);
        assertEquals(1, repositorio.count);
    }
}
