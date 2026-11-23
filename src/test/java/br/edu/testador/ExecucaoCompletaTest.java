package br.edu.testador;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExecucaoCompletaTest {

    private HttpServer servidor;
    private String urlBase;

    @TempDir
    Path pasta;

    @BeforeEach
    void subirApiFalsa() throws IOException {
        servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.createContext("/login", troca -> responder(troca, 200, "{\"token\": \"abc123\"}"));
        servidor.createContext("/itens", troca -> {
            String autorizacao = troca.getRequestHeaders().getFirst("Authorization");
            if (!"Bearer abc123".equals(autorizacao)) {
                responder(troca, 401, "{\"erro\": \"sem acesso\"}");
                return;
            }
            responder(troca, 200, "[{\"id\": 1, \"nome\": \"Caneta\"}, {\"id\": 2, \"nome\": \"Lápis\"}]");
        });
        servidor.createContext("/quebrado", troca ->
                responder(troca, 500, "java.lang.IllegalStateException: falhou\n\tat app.Servico.rodar(Servico.java:10)"));
        servidor.start();
        urlBase = "http://127.0.0.1:" + servidor.getAddress().getPort();
    }

    @AfterEach
    void derrubarApiFalsa() {
        servidor.stop(0);
    }

    private void responder(HttpExchange troca, int status, String corpo) throws IOException {
        byte[] bytes = corpo.getBytes(StandardCharsets.UTF_8);
        troca.getResponseHeaders().add("Content-Type", "application/json");
        troca.getResponseHeaders().add("X-Content-Type-Options", "nosniff");
        troca.sendResponseHeaders(status, bytes.length);
        troca.getResponseBody().write(bytes);
        troca.close();
    }

    private int rodar(String yaml, ByteArrayOutputStream saida) throws IOException {
        Path arquivo = pasta.resolve("suite.yaml");
        Files.writeString(arquivo, yaml);
        return Aplicacao.executar(new String[]{arquivo.toString(), "--url-base", urlBase, "--sem-cor",
                "--relatorios", pasta.resolve("relatorios").toString()}, new PrintStream(saida, true, StandardCharsets.UTF_8));
    }

    @Test
    void executaFluxoComCapturaEGeraRelatorios() throws IOException {
        String yaml = """
                nome: API falsa
                cenarios:
                  - nome: Login e listagem
                    passos:
                      - nome: Login
                        metodo: POST
                        caminho: /login
                        corpo:
                          usuario: teste
                        espera:
                          status: 200
                        capturar:
                          token: $.token
                      - nome: Listar
                        caminho: /itens
                        token: ${token}
                        espera:
                          status: 200
                          cabecalhos:
                            Content-Type: json
                          corpo:
                            - caminho: $
                              tamanho: 2
                            - caminho: $[*].nome
                              contem: Caneta
                      - nome: Carga
                        caminho: /itens
                        token: ${token}
                        repetir: 20
                        concorrencia: 4
                        espera:
                          status: 200
                seguranca:
                  rotasProtegidas:
                    - GET /itens
                  cabecalhos:
                    caminho: /itens
                    exigir: [X-Content-Type-Options]
                """;
        ByteArrayOutputStream saida = new ByteArrayOutputStream();
        int codigo = rodar(yaml, saida);

        String texto = saida.toString(StandardCharsets.UTF_8);
        assertEquals(0, codigo, texto);
        assertTrue(texto.contains("0 reprovados"), texto);
        assertTrue(Files.exists(pasta.resolve("relatorios/relatorio.html")));
        String junit = Files.readString(pasta.resolve("relatorios/junit.xml"));
        assertTrue(junit.contains("failures=\"0\""), junit);
    }

    @Test
    void reprovaEDevolveCodigoUmQuandoAlgoFalha() throws IOException {
        String yaml = """
                nome: API falsa
                cenarios:
                  - nome: Erros
                    passos:
                      - nome: Rota que vaza stack trace
                        caminho: /quebrado
                        espera:
                          status: 500
                """;
        ByteArrayOutputStream saida = new ByteArrayOutputStream();
        int codigo = rodar(yaml, saida);
        String texto = saida.toString(StandardCharsets.UTF_8);
        assertEquals(1, codigo, texto);
        assertTrue(texto.contains("detalhes internos"), texto);
        assertTrue(Files.readString(pasta.resolve("relatorios/junit.xml")).contains("<failure"));
    }

    @Test
    void arquivoInvalidoDevolveCodigoDois() throws IOException {
        ByteArrayOutputStream saida = new ByteArrayOutputStream();
        int codigo = rodar("nome: sem cenarios\ncampoInventado: 1\n", saida);
        assertEquals(2, codigo);
        assertTrue(saida.toString(StandardCharsets.UTF_8).contains("Campo desconhecido"));
    }
}
