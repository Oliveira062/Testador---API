package br.edu.testador.execucao;

public class VariavelNaoDefinidaException extends RuntimeException {

    public VariavelNaoDefinidaException(String nome) {
        super("Variável não definida: ${" + nome + "}. Defina em 'variaveis', capture em um passo anterior "
                + "ou passe com --var " + nome + "=valor.");
    }
}
