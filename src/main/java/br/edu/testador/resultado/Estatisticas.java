package br.edu.testador.resultado;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public record Estatisticas(int total, int falhas, long minimoMs, long mediaMs, long p95Ms, long maximoMs) {

    public static Estatisticas calcular(List<Long> duracoes, int falhas) {
        if (duracoes.isEmpty()) {
            return new Estatisticas(0, falhas, 0, 0, 0, 0);
        }
        List<Long> ordenadas = new ArrayList<>(duracoes);
        Collections.sort(ordenadas);
        long soma = 0;
        for (long d : ordenadas) {
            soma += d;
        }
        int indiceP95 = (int) Math.ceil(ordenadas.size() * 0.95) - 1;
        return new Estatisticas(ordenadas.size(), falhas, ordenadas.getFirst(), soma / ordenadas.size(),
                ordenadas.get(Math.max(0, indiceP95)), ordenadas.getLast());
    }
}
