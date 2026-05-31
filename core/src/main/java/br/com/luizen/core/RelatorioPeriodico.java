package br.com.luizen.core;

import java.util.List;

public class RelatorioPeriodico {
    Long mediaAvaliacoes;
    Long totalAvaliacoes; 
    Long porcentagemDeSatisfeitos;
    List<Feedback> feedbacks;

    public RelatorioPeriodico(List<Feedback> feedbacks) {
        this.feedbacks = feedbacks;
        calcularTotalAvaliacoes();
        calcularMediaAvaliacoes();
        calcularPorcentagemDeSatisfeitos();

    }

    private void calcularTotalAvaliacoes() {
        this.totalAvaliacoes = (long) feedbacks.size();
    }
    
    private void calcularMediaAvaliacoes() {
        if(feedbacks.isEmpty()) {
            this.mediaAvaliacoes = 0L;
            return;
        }

        Long somaAvaliacoes = 0L;
        for(Feedback feedback : feedbacks) {
            somaAvaliacoes += feedback.getNota();
        }
        this.mediaAvaliacoes = somaAvaliacoes / feedbacks.size();
    }

    private void calcularPorcentagemDeSatisfeitos() {
        if(feedbacks.isEmpty()) {
            this.porcentagemDeSatisfeitos = 0L;
            return;
        }
        
        Long totalSatisfeitos = 0L;
        for(Feedback feedback : feedbacks) {
            if(feedback.ehItemSatisfatorio()) {
                totalSatisfeitos++;
            }
        }
        this.porcentagemDeSatisfeitos = (totalSatisfeitos * 100) / feedbacks.size();
    }

    public Long getMediaAvaliacoes() {
        return mediaAvaliacoes;
    }

    public Long getTotalAvaliacoes() {
        return totalAvaliacoes;
    }

    public Long getPorcentagemDeSatisfeitos() {
        return porcentagemDeSatisfeitos;
    }

    public List<Feedback> getFeedbacks() {
        return feedbacks;
    }
}
