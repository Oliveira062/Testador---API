package br.edu.testador.relatorio;

import java.util.Locale;

final class Texto {

    private Texto() {
    }

    static String xml(String valor) {
        if (valor == null) {
            return "";
        }
        StringBuilder saida = new StringBuilder(valor.length());
        for (char c : valor.toCharArray()) {
            switch (c) {
                case '&' -> saida.append("&amp;");
                case '<' -> saida.append("&lt;");
                case '>' -> saida.append("&gt;");
                case '"' -> saida.append("&quot;");
                case '\'' -> saida.append("&#39;");
                default -> {
                    if (c >= 0x20 || c == '\n' || c == '\r' || c == '\t') {
                        saida.append(c);
                    }
                }
            }
        }
        return saida.toString();
    }

    static String segundos(long milissegundos) {
        return String.format(Locale.ROOT, "%.3f", milissegundos / 1000.0);
    }
}
