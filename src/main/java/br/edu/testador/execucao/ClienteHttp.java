package br.edu.testador.execucao;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

public class ClienteHttp {

    private final HttpClient cliente = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();

    public Resposta enviar(String metodo, String url, Map<String, String> cabecalhos, String corpo, long tempoLimiteMs)
            throws IOException, InterruptedException {
        HttpRequest.BodyPublisher publicador = corpo == null
                ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(corpo, StandardCharsets.UTF_8);

        HttpRequest.Builder construtor = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofMillis(tempoLimiteMs))
                .method(metodo, publicador);
        cabecalhos.forEach(construtor::header);

        long inicio = System.nanoTime();
        HttpResponse<String> resposta = cliente.send(construtor.build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        long duracao = (System.nanoTime() - inicio) / 1_000_000;

        return new Resposta(resposta.statusCode(), resposta.headers().map(), resposta.body(), duracao);
    }
}
