package br.edu.testador.resultado;

import java.util.List;

public record ResultadoPasso(String nome, String metodo, String url, Integer status, long duracaoMs,
                             List<Verificacao> verificacoes, String erro, Estatisticas estatisticas) {

    public boolean aprovado() {
        return erro == null && verificacoes.stream().allMatch(Verificacao::aprovada);
    }

    public List<Verificacao> falhas() {
        return verificacoes.stream().filter(v -> !v.aprovada()).toList();
    }
}
