package br.com.luizen.lambda.postarFeedback;

import java.util.List;

public class FeedbackOutput {
    
    private String mensagem;
    private List<String> erros;

    public String getMensagem() {
        return mensagem;
    }

    public List<String> getErros() {
        return erros;
    }

    public static FeedbackOutput feedbackComErro(List<String> erros) {
        FeedbackOutput feedbackOutput = new FeedbackOutput();
        feedbackOutput.mensagem = "Erro ao postar feedback";
        feedbackOutput.erros = erros;
        return feedbackOutput;
    }

    public static FeedbackOutput feedbackPostado(String nota) {
        FeedbackOutput feedbackOutput = new FeedbackOutput();
        feedbackOutput.mensagem = "Feedback recebido com nota: " + nota;
        return feedbackOutput;
    }

}
