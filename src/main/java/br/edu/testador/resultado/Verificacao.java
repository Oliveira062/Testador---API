package br.edu.testador.resultado;

public record Verificacao(String descricao, boolean aprovada, String detalhe) {

    public static Verificacao ok(String descricao) {
        return new Verificacao(descricao, true, null);
    }

    public static Verificacao falha(String descricao, String detalhe) {
        return new Verificacao(descricao, false, detalhe);
    }
}
