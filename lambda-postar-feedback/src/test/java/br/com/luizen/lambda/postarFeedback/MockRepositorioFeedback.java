package br.com.luizen.lambda.postarFeedback;

import br.com.luizen.core.Feedback;
import br.com.luizen.core.ports.IRepositorioFeedback;

public class MockRepositorioFeedback implements IRepositorioFeedback {
    public int count = 0;

    @Override
    public void salvar(Feedback feedback) {
        count++;
    }
}
