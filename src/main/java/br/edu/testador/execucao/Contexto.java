package br.edu.testador.execucao;

import java.util.LinkedHashMap;
import java.util.Map;

public class Contexto {

    private final Map<String, Object> variaveis = new LinkedHashMap<>();

    public void definir(String nome, Object valor) {
        variaveis.put(nome, valor);
    }

    public boolean possui(String nome) {
        return variaveis.containsKey(nome);
    }

    public Object obter(String nome) {
        return variaveis.get(nome);
    }

    public Map<String, Object> todas() {
        return Map.copyOf(variaveis);
    }
}
