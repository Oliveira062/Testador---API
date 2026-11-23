package br.edu.testador.modelo;

import java.util.List;

public record Cenario(String nome, List<String> tags, List<Passo> passos) {

    public Cenario {
        tags = tags == null ? List.of() : tags;
        passos = passos == null ? List.of() : passos;
    }

    public boolean possuiAlgumaTag(List<String> filtro) {
        if (filtro == null || filtro.isEmpty()) {
            return true;
        }
        return tags.stream().anyMatch(filtro::contains);
    }
}
