package br.com.luizen.core;

import java.util.ArrayList;
import java.util.List;

public class Feedback {
    
    String descricao;
    Long nota;

    public static Feedback criar(String descricao, Long nota) {
        Feedback feedback = new Feedback();
        feedback.descricao = descricao;
        feedback.nota = nota;
        return feedback;
    }

    public boolean ehItemCritico(){
        return this.nota != null && this.nota < 3;
    }

    public List<String> validar() {
        List<String> erros = new ArrayList<>();
        if(this.descricao == null || this.descricao.isEmpty()){
            erros.add("Descrição é obrigatória");
        }

        if(this.nota == null){
            erros.add("Nota é obrigatória");
        }

        if(this.nota != null && (this.nota < 1 || this.nota > 5)){
            erros.add("Nota deve ser entre 1 e 5");
        }

        return erros.isEmpty() ? null : erros;
    }
    
}
