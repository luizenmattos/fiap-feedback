package br.com.luizen.lambda.gerarRelatorio;

public class RelatorioOutput {

    private Long mediaAvaliacoes;
    private Long totalAvaliacoes;
    private Long porcentagemDeSatisfeitos;
    private String mensagem;

    public Long getMediaAvaliacoes() {
        return mediaAvaliacoes;
    }

    public Long getTotalAvaliacoes() {
        return totalAvaliacoes;
    }

    public Long getPorcentagemDeSatisfeitos() {
        return porcentagemDeSatisfeitos;
    }

    public String getMensagem() {
        return mensagem;
    }

    public static RelatorioOutput relatorioGerado(Long totalAvaliacoes, Long mediaAvaliacoes, Long porcentagemDeSatisfeitos) {
        RelatorioOutput output = new RelatorioOutput();
        output.totalAvaliacoes = totalAvaliacoes;
        output.mediaAvaliacoes = mediaAvaliacoes;
        output.porcentagemDeSatisfeitos = porcentagemDeSatisfeitos;
        output.mensagem = "Relatório gerado com sucesso";
        return output;
    }

    public static RelatorioOutput relatorioComErro(String mensagem) {
        RelatorioOutput output = new RelatorioOutput();
        output.mensagem = mensagem;
        return output;
    }

    public static RelatorioOutput naoAutorizado() {
        RelatorioOutput output = new RelatorioOutput();
        output.mensagem = "Não autorizado";
        return output;
    }
}
