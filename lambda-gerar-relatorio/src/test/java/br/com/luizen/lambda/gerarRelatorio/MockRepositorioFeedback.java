package br.com.luizen.lambda.gerarRelatorio;

import java.util.Date;
import java.util.List;

import br.com.luizen.core.Feedback;
import br.com.luizen.core.ports.IRepositorioFeedback;

public class MockRepositorioFeedback implements IRepositorioFeedback {

    private final List<Feedback> feedbacksRetornados;

    public MockRepositorioFeedback(List<Feedback> feedbacksRetornados) {
        this.feedbacksRetornados = feedbacksRetornados;
    }

    @Override
    public void salvar(Feedback feedback) {}

    @Override
    public List<Feedback> obterFeedbacks(Date dataInicial, Date dataFinal) {
        return feedbacksRetornados;
    }
}
