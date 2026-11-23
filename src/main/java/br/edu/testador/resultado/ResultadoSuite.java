package br.edu.testador.resultado;

import java.util.List;

public record ResultadoSuite(String nome, String arquivo, List<ResultadoCenario> cenarios, long duracaoMs) {

    public boolean aprovada() {
        return cenarios.stream().allMatch(ResultadoCenario::aprovado);
    }

    public int totalPassos() {
        return cenarios.stream().mapToInt(c -> c.passos().size()).sum();
    }

    public int passosReprovados() {
        return (int) cenarios.stream().flatMap(c -> c.passos().stream()).filter(p -> !p.aprovado()).count();
    }
}
