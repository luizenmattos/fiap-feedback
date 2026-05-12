package br.com.luizen.core;

import java.util.Date;
import java.util.List;

import br.com.luizen.core.ports.IRepositorioFeedback;

public class GerarRelatorioPeriodico {
    
    public static RelatorioPeriodico executar(Date dataInicial, Date dataFinal, IRepositorioFeedback repositorioFeedback) {
        List<Feedback> feedbacks = repositorioFeedback.obterFeedbacks(dataInicial, dataFinal);

        return new RelatorioPeriodico(feedbacks);
    } 
}
