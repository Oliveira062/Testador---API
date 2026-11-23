package br.edu.testador;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record Opcoes(List<String> entradas, String urlBase, Map<String, String> variaveis, List<String> tags,
                     String pastaRelatorios, boolean pararNaFalha, boolean cores, boolean ajuda) {

    public static Opcoes ler(String[] args) {
        List<String> entradas = new ArrayList<>();
        Map<String, String> variaveis = new LinkedHashMap<>();
        List<String> tags = new ArrayList<>();
        String urlBase = null;
        String pasta = "relatorios";
        boolean pararNaFalha = false;
        boolean cores = System.console() != null;
        boolean ajuda = args.length == 0;

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            switch (arg) {
                case "--url-base" -> urlBase = valorDe(args, ++i, arg);
                case "--relatorios" -> pasta = valorDe(args, ++i, arg);
                case "--tags" -> tags.addAll(List.of(valorDe(args, ++i, arg).split(",")));
                case "--parar-na-falha" -> pararNaFalha = true;
                case "--sem-cor" -> cores = false;
                case "--ajuda", "-h", "--help" -> ajuda = true;
                case "--var" -> {
                    String par = valorDe(args, ++i, arg);
                    int igual = par.indexOf('=');
                    if (igual <= 0) {
                        throw new IllegalArgumentException("Use --var nome=valor (recebido: " + par + ")");
                    }
                    variaveis.put(par.substring(0, igual).trim(), par.substring(igual + 1));
                }
                default -> {
                    if (arg.startsWith("--")) {
                        throw new IllegalArgumentException("Opção desconhecida: " + arg);
                    }
                    entradas.add(arg);
                }
            }
        }
        tags.replaceAll(String::trim);
        tags.removeIf(String::isEmpty);
        return new Opcoes(entradas, urlBase, variaveis, tags, pasta, pararNaFalha, cores, ajuda);
    }

    private static String valorDe(String[] args, int indice, String opcao) {
        if (indice >= args.length) {
            throw new IllegalArgumentException("A opção " + opcao + " precisa de um valor.");
        }
        return args[indice];
    }

    public static String textoDeAjuda() {
        return """
                testador-api - executa testes de API REST descritos em arquivos YAML ou JSON

                Uso:
                  java -jar testador-api.jar <arquivo-ou-pasta>... [opções]

                Opções:
                  --url-base URL        substitui a urlBase definida nos arquivos
                  --var nome=valor      define ou sobrescreve uma variável (pode repetir)
                  --tags a,b            executa só os cenários com alguma dessas tags
                  --relatorios PASTA    onde salvar relatorio.html e junit.xml (padrão: relatorios)
                  --parar-na-falha      interrompe a suíte no primeiro passo reprovado
                  --sem-cor             saída sem cores (útil em CI)
                  --ajuda               mostra esta mensagem

                Códigos de saída:
                  0 todos os passos aprovados | 1 houve reprovação | 2 erro de uso ou de arquivo
                """;
    }
}
