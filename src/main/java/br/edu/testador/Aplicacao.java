package br.edu.testador;

import br.edu.testador.execucao.ClienteHttp;
import br.edu.testador.execucao.ExecutorDePasso;
import br.edu.testador.execucao.ExecutorDeSuite;
import br.edu.testador.execucao.Interpolador;
import br.edu.testador.execucao.VarreduraDeSeguranca;
import br.edu.testador.execucao.VerificadorDeRespostas;
import br.edu.testador.leitura.ArquivoInvalidoException;
import br.edu.testador.leitura.LeitorDeSuites;
import br.edu.testador.modelo.Suite;
import br.edu.testador.relatorio.RelatorioConsole;
import br.edu.testador.relatorio.RelatorioHtml;
import br.edu.testador.relatorio.RelatorioJUnit;
import br.edu.testador.resultado.ResultadoSuite;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class Aplicacao {

    public static void main(String[] args) {
        System.exit(executar(args, new PrintStream(System.out, true, StandardCharsets.UTF_8)));
    }

    public static int executar(String[] args, PrintStream saida) {
        Opcoes opcoes;
        try {
            opcoes = Opcoes.ler(args);
        } catch (IllegalArgumentException e) {
            saida.println("Erro: " + e.getMessage());
            saida.println();
            saida.println(Opcoes.textoDeAjuda());
            return 2;
        }
        if (opcoes.ajuda() || opcoes.entradas().isEmpty()) {
            saida.println(Opcoes.textoDeAjuda());
            return opcoes.ajuda() ? 0 : 2;
        }

        LeitorDeSuites leitor = new LeitorDeSuites();
        List<Path> arquivos;
        List<Suite> suites = new ArrayList<>();
        try {
            arquivos = leitor.localizarArquivos(opcoes.entradas());
            for (Path arquivo : arquivos) {
                Suite suite = leitor.ler(arquivo);
                suites.add(opcoes.urlBase() == null ? suite : suite.comUrlBase(opcoes.urlBase()));
            }
        } catch (ArquivoInvalidoException | IOException e) {
            saida.println("Erro: " + e.getMessage());
            return 2;
        }

        ClienteHttp cliente = new ClienteHttp();
        Interpolador interpolador = new Interpolador();
        ExecutorDeSuite executor = new ExecutorDeSuite(
                new ExecutorDePasso(cliente, interpolador, new VerificadorDeRespostas()),
                new VarreduraDeSeguranca(cliente, interpolador),
                interpolador);
        RelatorioConsole console = new RelatorioConsole(saida, opcoes.cores());

        List<ResultadoSuite> resultados = new ArrayList<>();
        for (int i = 0; i < suites.size(); i++) {
            Suite suite = suites.get(i);
            String arquivo = arquivos.get(i).getFileName().toString();
            console.inicioDaSuite(suite.nome() == null ? arquivo : suite.nome(), arquivo);
            resultados.add(executor.executar(suite, arquivo, opcoes.variaveis(), opcoes.tags(),
                    opcoes.pararNaFalha(), console::passo));
        }
        console.resumo(resultados);

        try {
            Path pasta = Path.of(opcoes.pastaRelatorios());
            Path html = new RelatorioHtml().gerar(resultados, pasta);
            Path junit = new RelatorioJUnit().gerar(resultados, pasta);
            saida.println("Relatórios: " + html.toAbsolutePath() + " e " + junit.toAbsolutePath());
        } catch (IOException e) {
            saida.println("Aviso: não foi possível salvar os relatórios: " + e.getMessage());
        }

        return resultados.stream().allMatch(ResultadoSuite::aprovada) ? 0 : 1;
    }
}
