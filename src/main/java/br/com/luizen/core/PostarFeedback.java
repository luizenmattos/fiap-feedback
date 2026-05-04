package br.com.luizen.core;

import java.util.List;

public class PostarFeedback {

    public static List<String> executar(String descricao, Long nota) {
        Feedback feedback = Feedback.criar(descricao, nota);
        
        List<String> erros = feedback.validar();
        if(erros != null){
            return erros;
        }

        if(feedback.ehItemCritico()){
            // Enviar email para o time de qualidade
        }

        return null;
    }
    
}
