package br.com.luizen.core.ports;

import java.util.Date;
import java.util.List;

import br.com.luizen.core.Feedback;

public interface IRepositorioFeedback {
    
    void salvar(Feedback feedback);

    List<Feedback> obterFeedbacks(Date dataInicial, Date dataFinal);
}
