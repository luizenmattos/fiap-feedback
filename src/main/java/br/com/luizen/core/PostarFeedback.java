package br.com.luizen.core;

import java.util.List;

import br.com.luizen.core.ports.IPublicadorEventos;

public class PostarFeedback {

    public static List<String> executar(String descricao, Long nota, IPublicadorEventos publicadorEventos) {
        Feedback feedback = Feedback.criar(descricao, nota);
        
        List<String> erros = feedback.validar();
        if(erros != null){
            return erros;
        }

        publicadorEventos.publicar(
            new FeedbackPostado(feedback)
        );

        return null;
    }
    
}
