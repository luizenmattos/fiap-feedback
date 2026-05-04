package br.com.luizen.lambda.postarFeedback;

import br.com.luizen.core.ports.IPublicadorEventos;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import jakarta.annotation.Priority;

@ApplicationScoped
@Alternative
@Priority(1)
public class MockPublicadorEventos implements IPublicadorEventos {
    @Override
    public void publicar(Object evento) {
        // Mock que não faz nada.
    }
}
