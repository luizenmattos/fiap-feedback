package br.com.luizen.lambda.postarFeedback;

import java.util.List;

import org.jboss.logging.Logger;

import br.com.luizen.core.PostarFeedback;
import br.com.luizen.core.ValidadorAutenticacao;
import br.com.luizen.core.ports.IPublicadorEventos;
import br.com.luizen.core.ports.IRepositorioFeedback;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/feedback")
public class PostarFeedbackResource {

    private static final Logger LOG = Logger.getLogger(PostarFeedbackResource.class);

    @Inject
    IPublicadorEventos publicadorEventos;

    @Inject
    IRepositorioFeedback repositorioFeedback;

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response postar(FeedbackInput input) {
        LOG.infof("Iniciando postagem de feedback. nota=%d", input.nota);

        if (!ValidadorAutenticacao.validar(input.apiKey)) {
            LOG.warn("Tentativa de acesso não autorizado em postar-feedback.");
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(FeedbackOutput.naoAutorizado())
                    .build();
        }

        List<String> erro = PostarFeedback.executar(input.descricao, input.nota, publicadorEventos, repositorioFeedback);
        if (erro != null) {
            LOG.warnf("Feedback rejeitado por erros de validação: %s", erro);
            return Response.status(422)
                    .entity(FeedbackOutput.feedbackComErro(erro))
                    .build();
        }

        LOG.info("Feedback postado com sucesso.");
        return Response.ok(FeedbackOutput.feedbackPostado(input.nota.toString())).build();
    }
}
