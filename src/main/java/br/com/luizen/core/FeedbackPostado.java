package br.com.luizen.core;

public record FeedbackPostado(String descricao, Long nota, Boolean ehItemCritico) {

    public FeedbackPostado(Feedback feedback) {
        this(feedback.getDescricao(), feedback.getNota(), feedback.ehItemCritico());
    }

}
