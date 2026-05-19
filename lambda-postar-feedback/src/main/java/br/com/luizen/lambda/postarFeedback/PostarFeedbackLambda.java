package br.com.luizen.lambda.postarFeedback;

import java.util.List;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

import org.jboss.logging.Logger;

import br.com.luizen.core.PostarFeedback;
import br.com.luizen.core.ValidadorAutenticacao;
import br.com.luizen.core.ports.IPublicadorEventos;
import br.com.luizen.core.ports.IRepositorioFeedback;
import jakarta.inject.Named;
import jakarta.inject.Inject;

@Named("postarFeedback")
public class PostarFeedbackLambda implements RequestHandler<FeedbackInput, FeedbackOutput> {

    private static final Logger LOG = Logger.getLogger(PostarFeedbackLambda.class);

    @Inject
    IPublicadorEventos publicadorEventos;

    @Inject
    IRepositorioFeedback repositorioFeedback;

    @Override
    public FeedbackOutput handleRequest(FeedbackInput input, Context context) {
        LOG.infof("Iniciando postagem de feedback. nota=%d", input.nota);

        if (!ValidadorAutenticacao.validar(input.apiKey)) {
            LOG.warn("Tentativa de acesso não autorizado em postar-feedback.");
            return FeedbackOutput.naoAutorizado();
        }

        List<String> erro = PostarFeedback.executar(input.descricao, input.nota, publicadorEventos, repositorioFeedback);
        if (erro != null) {
            LOG.warnf("Feedback rejeitado por erros de validação: %s", erro);
            return FeedbackOutput.feedbackComErro(erro);
        }

        LOG.info("Feedback postado com sucesso.");
        return FeedbackOutput.feedbackPostado(input.nota.toString());
    }
}
