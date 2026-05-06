package br.com.luizen.lambda;

import br.com.luizen.core.Feedback;
import br.com.luizen.core.ports.IRepositorioFeedback;
import io.quarkus.test.Mock;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;

@Mock
@ApplicationScoped
@Alternative
@Priority(1)
public class MockRepositorioFeedbackCDI implements IRepositorioFeedback {

    @Override
    public void salvar(Feedback feedback) {
        // sem operação em testes
    }
}
