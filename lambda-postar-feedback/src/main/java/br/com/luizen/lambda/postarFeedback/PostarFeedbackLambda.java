package br.com.luizen.lambda.postarFeedback;

import java.util.List;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

import br.com.luizen.core.PostarFeedback;
import br.com.luizen.core.ports.IPublicadorEventos;
import jakarta.inject.Named;
import jakarta.inject.Inject;

@Named("postarFeedback")
public class PostarFeedbackLambda implements RequestHandler<FeedbackInput, FeedbackOutput> {

    @Inject
    IPublicadorEventos publicadorEventos;

    @Override
    public FeedbackOutput handleRequest(FeedbackInput input, Context context) {

        List<String> erro = PostarFeedback.executar(input.descricao, input.nota, publicadorEventos);
        if(erro != null){
            return FeedbackOutput.feedbackComErro(erro);
        }

        return FeedbackOutput.feedbackPostado(input.nota.toString());
    }
}
