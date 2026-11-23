package br.edu.testador.modelo;

import java.util.List;

public record VerificacaoCabecalhos(String caminho, List<String> exigir) {

    public VerificacaoCabecalhos {
        caminho = caminho == null ? "/" : caminho;
        exigir = exigir == null || exigir.isEmpty()
                ? List.of("X-Content-Type-Options", "X-Frame-Options")
                : exigir;
    }
}
