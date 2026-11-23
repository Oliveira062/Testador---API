package br.edu.testador.execucao;

import java.util.List;
import java.util.Map;

public record Resposta(int status, Map<String, List<String>> cabecalhos, String corpo, long duracaoMs) {

    public String cabecalho(String nome) {
        for (Map.Entry<String, List<String>> entrada : cabecalhos.entrySet()) {
            if (entrada.getKey() != null && entrada.getKey().equalsIgnoreCase(nome)) {
                return String.join(", ", entrada.getValue());
            }
        }
        return null;
    }
}
