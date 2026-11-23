package br.edu.testador.relatorio;

import br.edu.testador.resultado.Estatisticas;
import br.edu.testador.resultado.ResultadoPasso;
import br.edu.testador.resultado.ResultadoSuite;
import br.edu.testador.resultado.Verificacao;

import java.io.PrintStream;
import java.util.List;

public class RelatorioConsole {

    private static final String VERDE = "\u001B[32m";
    private static final String VERMELHO = "\u001B[31m";
    private static final String CINZA = "\u001B[90m";
    private static final String NEGRITO = "\u001B[1m";
    private static final String NORMAL = "\u001B[0m";

    private final PrintStream saida;
    private final boolean cores;

    public RelatorioConsole(PrintStream saida, boolean cores) {
        this.saida = saida;
        this.cores = cores;
    }

    public void inicioDaSuite(String nome, String arquivo) {
        saida.println();
        saida.println(cor(NEGRITO, "== " + nome) + cor(CINZA, "  (" + arquivo + ")"));
    }

    public void passo(ResultadoPasso passo) {
        String marca = passo.aprovado() ? cor(VERDE, "[OK]   ") : cor(VERMELHO, "[FALHA]");
        String status = passo.status() == null ? "---" : String.valueOf(passo.status());
        saida.printf("%s %s %s%n", marca, passo.nome(), cor(CINZA, "(" + status + ", " + passo.duracaoMs() + " ms)"));
        if (passo.estatisticas() != null) {
            Estatisticas e = passo.estatisticas();
            saida.println(cor(CINZA, String.format("        %d req | mín %d ms | média %d ms | p95 %d ms | máx %d ms",
                    e.total(), e.minimoMs(), e.mediaMs(), e.p95Ms(), e.maximoMs())));
        }
        if (passo.erro() != null) {
            saida.println(cor(VERMELHO, "        " + passo.erro()));
        }
        for (Verificacao falha : passo.falhas()) {
            saida.println(cor(VERMELHO, "        esperado: " + falha.descricao()));
            saida.println(cor(VERMELHO, "        obtido:   " + falha.detalhe()));
        }
    }

    public void resumo(List<ResultadoSuite> resultados) {
        int total = resultados.stream().mapToInt(ResultadoSuite::totalPassos).sum();
        int reprovados = resultados.stream().mapToInt(ResultadoSuite::passosReprovados).sum();
        long tempo = resultados.stream().mapToLong(ResultadoSuite::duracaoMs).sum();
        saida.println();
        String linha = String.format("Resultado: %d passos, %d aprovados, %d reprovados em %d ms",
                total, total - reprovados, reprovados, tempo);
        saida.println(cor(NEGRITO, cor(reprovados == 0 ? VERDE : VERMELHO, linha)));
    }

    private String cor(String codigo, String texto) {
        return cores ? codigo + texto + NORMAL : texto;
    }
}
