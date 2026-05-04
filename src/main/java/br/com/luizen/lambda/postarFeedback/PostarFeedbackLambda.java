package br.com.luizen.lambda.postarFeedback;

import java.util.List;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

import br.com.luizen.core.PostarFeedback;

public class PostarFeedbackLambda implements RequestHandler<FeedbackInput, FeedbackOutput> {

    @Override
    public FeedbackOutput handleRequest(FeedbackInput input, Context context) {

        List<String> erro = PostarFeedback.executar(input.descricao, input.nota);
        if(erro != null){
            return FeedbackOutput.feedbackComErro(erro);
        }

        return FeedbackOutput.feedbackPostado(input.nota.toString());
    }
}
