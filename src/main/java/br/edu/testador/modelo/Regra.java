package br.edu.testador.modelo;

public record Regra(
        String caminho,
        Object igual,
        Object diferente,
        Boolean existe,
        Object contem,
        Double maiorQue,
        Double menorQue,
        Integer tamanho,
        String tipo,
        String regex
) {
}
