package br.edu.testador.modelo;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record Suite(
        String nome,
        String urlBase,
        Long tempoLimiteMs,
        Map<String, Object> variaveis,
        Map<String, String> cabecalhos,
        Boolean verificarVazamento,
        Seguranca seguranca,
        List<Cenario> cenarios
) {

    public Suite {
        tempoLimiteMs = tempoLimiteMs == null ? 10_000L : tempoLimiteMs;
        variaveis = variaveis == null ? new LinkedHashMap<>() : variaveis;
        cabecalhos = cabecalhos == null ? Map.of() : cabecalhos;
        verificarVazamento = verificarVazamento == null || verificarVazamento;
        cenarios = cenarios == null ? List.of() : cenarios;
    }

    public Suite comUrlBase(String novaUrl) {
        return new Suite(nome, novaUrl, tempoLimiteMs, variaveis, cabecalhos, verificarVazamento, seguranca, cenarios);
    }
}
