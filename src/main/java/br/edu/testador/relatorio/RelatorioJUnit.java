package br.edu.testador.relatorio;

import br.edu.testador.resultado.ResultadoCenario;
import br.edu.testador.resultado.ResultadoPasso;
import br.edu.testador.resultado.ResultadoSuite;
import br.edu.testador.resultado.Verificacao;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

public class RelatorioJUnit implements Relatorio {

    @Override
    public Path gerar(List<ResultadoSuite> resultados, Path pasta) throws IOException {
        int total = resultados.stream().mapToInt(ResultadoSuite::totalPassos).sum();
        int falhas = resultados.stream().mapToInt(ResultadoSuite::passosReprovados).sum();
        long tempo = resultados.stream().mapToLong(ResultadoSuite::duracaoMs).sum();

        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append(String.format("<testsuites name=\"testador-api\" tests=\"%d\" failures=\"%d\" time=\"%s\">%n",
                total, falhas, Texto.segundos(tempo)));

        for (ResultadoSuite suite : resultados) {
            xml.append(String.format("  <testsuite name=\"%s\" tests=\"%d\" failures=\"%d\" time=\"%s\">%n",
                    Texto.xml(suite.nome()), suite.totalPassos(), suite.passosReprovados(),
                    Texto.segundos(suite.duracaoMs())));
            for (ResultadoCenario cenario : suite.cenarios()) {
                for (ResultadoPasso passo : cenario.passos()) {
                    xml.append(String.format("    <testcase classname=\"%s\" name=\"%s\" time=\"%s\"",
                            Texto.xml(suite.nome() + "." + cenario.nome()), Texto.xml(passo.nome()),
                            Texto.segundos(passo.duracaoMs())));
                    if (passo.aprovado()) {
                        xml.append("/>\n");
                        continue;
                    }
                    xml.append(">\n");
                    String mensagem = passo.erro() != null ? passo.erro() : passo.falhas().stream()
                            .map(Verificacao::descricao).collect(Collectors.joining("; "));
                    String detalhes = passo.falhas().stream()
                            .map(v -> "esperado: " + v.descricao() + " | obtido: " + v.detalhe())
                            .collect(Collectors.joining("\n"));
                    xml.append(String.format("      <failure message=\"%s\">%s</failure>%n",
                            Texto.xml(mensagem), Texto.xml(detalhes)));
                    xml.append("    </testcase>\n");
                }
            }
            xml.append("  </testsuite>\n");
        }
        xml.append("</testsuites>\n");

        Files.createDirectories(pasta);
        Path arquivo = pasta.resolve("junit.xml");
        Files.writeString(arquivo, xml.toString(), StandardCharsets.UTF_8);
        return arquivo;
    }
}
