package br.edu.testador.modelo;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.util.List;
import java.util.Map;

public record Espera(
        @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
        List<Integer> status,
        Long tempoMaximoMs,
        Long p95MaximoMs,
        String corpoContem,
        String corpoNaoContem,
        Map<String, String> cabecalhos,
        List<Regra> corpo
) {

    public Espera {
        status = status == null ? List.of() : status;
        cabecalhos = cabecalhos == null ? Map.of() : cabecalhos;
        corpo = corpo == null ? List.of() : corpo;
    }

    public static Espera padrao() {
        return new Espera(null, null, null, null, null, null, null);
    }
}
