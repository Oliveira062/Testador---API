package br.edu.testador.modelo;

import java.util.Map;

public record Passo(
        String nome,
        String metodo,
        String caminho,
        Map<String, String> cabecalhos,
        String token,
        Object corpo,
        Espera espera,
        Map<String, String> capturar,
        Integer repetir,
        Integer concorrencia,
        Long pausaMs
) {

    public Passo {
        metodo = metodo == null ? "GET" : metodo.trim().toUpperCase();
        cabecalhos = cabecalhos == null ? Map.of() : cabecalhos;
        espera = espera == null ? Espera.padrao() : espera;
        capturar = capturar == null ? Map.of() : capturar;
        repetir = repetir == null || repetir < 1 ? 1 : repetir;
        concorrencia = concorrencia == null || concorrencia < 1 ? 1 : concorrencia;
        pausaMs = pausaMs == null ? 0L : pausaMs;
    }
}
