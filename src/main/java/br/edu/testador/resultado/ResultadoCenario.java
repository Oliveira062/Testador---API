package br.edu.testador.resultado;

import java.util.List;

public record ResultadoCenario(String nome, List<ResultadoPasso> passos) {

    public boolean aprovado() {
        return passos.stream().allMatch(ResultadoPasso::aprovado);
    }

    public long duracaoMs() {
        return passos.stream().mapToLong(ResultadoPasso::duracaoMs).sum();
    }
}
