package br.edu.testador.relatorio;

import br.edu.testador.resultado.Estatisticas;
import br.edu.testador.resultado.ResultadoCenario;
import br.edu.testador.resultado.ResultadoPasso;
import br.edu.testador.resultado.ResultadoSuite;
import br.edu.testador.resultado.Verificacao;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class RelatorioHtml implements Relatorio {

    private static final String ESTILO = """
            body { font-family: system-ui, -apple-system, "Segoe UI", Roboto, sans-serif; margin: 0;
                   background: #f5f6f8; color: #1f2933; }
            header { background: #1f2933; color: #fff; padding: 24px 32px; }
            header h1 { margin: 0 0 4px; font-size: 22px; }
            header p { margin: 0; color: #b8c2cc; font-size: 14px; }
            main { padding: 24px 32px; max-width: 1100px; }
            .numeros { display: flex; gap: 16px; margin-bottom: 24px; flex-wrap: wrap; }
            .numero { background: #fff; border-radius: 8px; padding: 16px 20px; min-width: 140px;
                      border: 1px solid #e4e7eb; }
            .numero strong { display: block; font-size: 26px; }
            .numero span { color: #616e7c; font-size: 13px; }
            .ok strong { color: #1e7e34; }
            .falha strong { color: #c62828; }
            section { background: #fff; border: 1px solid #e4e7eb; border-radius: 8px; margin-bottom: 20px; }
            section h2 { font-size: 17px; margin: 0; padding: 14px 18px; border-bottom: 1px solid #e4e7eb; }
            section h2 small { color: #616e7c; font-weight: normal; }
            h3 { font-size: 14px; margin: 16px 18px 6px; color: #3e4c59; }
            table { width: 100%; border-collapse: collapse; font-size: 13px; }
            td { padding: 8px 18px; border-top: 1px solid #f0f2f4; vertical-align: top; }
            td.marca { width: 70px; font-weight: 600; }
            tr.aprovado td.marca { color: #1e7e34; }
            tr.reprovado td.marca { color: #c62828; }
            tr.reprovado { background: #fff5f5; }
            td.info { color: #616e7c; white-space: nowrap; width: 160px; text-align: right; }
            .url { color: #7b8794; font-size: 12px; word-break: break-all; }
            .detalhe { color: #c62828; margin-top: 6px; font-size: 12px; }
            .estatistica { color: #3e4c59; margin-top: 4px; font-size: 12px; }
            """;

    @Override
    public Path gerar(List<ResultadoSuite> resultados, Path pasta) throws IOException {
        int total = resultados.stream().mapToInt(ResultadoSuite::totalPassos).sum();
        int reprovados = resultados.stream().mapToInt(ResultadoSuite::passosReprovados).sum();
        long tempo = resultados.stream().mapToLong(ResultadoSuite::duracaoMs).sum();
        String quando = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));

        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>\n<html lang=\"pt-BR\">\n<head>\n<meta charset=\"UTF-8\">\n")
                .append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n")
                .append("<title>Relatório de testes de API</title>\n<style>").append(ESTILO).append("</style>\n")
                .append("</head>\n<body>\n<header>\n<h1>Relatório de testes de API</h1>\n")
                .append("<p>Gerado em ").append(quando).append(" pelo testador-api</p>\n</header>\n<main>\n");

        html.append("<div class=\"numeros\">\n")
                .append(numero(String.valueOf(total), "passos executados", ""))
                .append(numero(String.valueOf(total - reprovados), "aprovados", "ok"))
                .append(numero(String.valueOf(reprovados), "reprovados", reprovados > 0 ? "falha" : ""))
                .append(numero(tempo + " ms", "tempo total", ""))
                .append("</div>\n");

        for (ResultadoSuite suite : resultados) {
            html.append("<section>\n<h2>").append(Texto.xml(suite.nome())).append(" <small>")
                    .append(Texto.xml(suite.arquivo())).append(" &middot; ")
                    .append(suite.totalPassos() - suite.passosReprovados()).append("/").append(suite.totalPassos())
                    .append(" aprovados</small></h2>\n");
            for (ResultadoCenario cenario : suite.cenarios()) {
                html.append("<h3>").append(Texto.xml(cenario.nome())).append("</h3>\n<table>\n");
                for (ResultadoPasso passo : cenario.passos()) {
                    html.append(linha(passo));
                }
                html.append("</table>\n");
            }
            html.append("</section>\n");
        }
        html.append("</main>\n</body>\n</html>\n");

        Files.createDirectories(pasta);
        Path arquivo = pasta.resolve("relatorio.html");
        Files.writeString(arquivo, html.toString(), StandardCharsets.UTF_8);
        return arquivo;
    }

    private String numero(String valor, String rotulo, String classe) {
        return "<div class=\"numero " + classe + "\"><strong>" + valor + "</strong><span>" + rotulo + "</span></div>\n";
    }

    private String linha(ResultadoPasso passo) {
        StringBuilder html = new StringBuilder();
        html.append("<tr class=\"").append(passo.aprovado() ? "aprovado" : "reprovado").append("\">")
                .append("<td class=\"marca\">").append(passo.aprovado() ? "OK" : "FALHA").append("</td><td>")
                .append(Texto.xml(passo.nome()));
        if (passo.url() != null) {
            html.append("<div class=\"url\">").append(Texto.xml(passo.metodo())).append(" ")
                    .append(Texto.xml(passo.url())).append("</div>");
        }
        Estatisticas e = passo.estatisticas();
        if (e != null) {
            html.append("<div class=\"estatistica\">").append(e.total()).append(" requisições &middot; mín ")
                    .append(e.minimoMs()).append(" ms &middot; média ").append(e.mediaMs()).append(" ms &middot; p95 ")
                    .append(e.p95Ms()).append(" ms &middot; máx ").append(e.maximoMs()).append(" ms</div>");
        }
        if (passo.erro() != null) {
            html.append("<div class=\"detalhe\">").append(Texto.xml(passo.erro())).append("</div>");
        }
        for (Verificacao falha : passo.falhas()) {
            html.append("<div class=\"detalhe\">Esperado: ").append(Texto.xml(falha.descricao()))
                    .append("<br>Obtido: ").append(Texto.xml(falha.detalhe())).append("</div>");
        }
        String status = passo.status() == null ? "sem resposta" : "HTTP " + passo.status();
        html.append("</td><td class=\"info\">").append(status).append(" &middot; ").append(passo.duracaoMs())
                .append(" ms</td></tr>\n");
        return html.toString();
    }
}
