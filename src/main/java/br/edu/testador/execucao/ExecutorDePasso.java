package br.edu.testador.execucao;

import br.edu.testador.modelo.Espera;
import br.edu.testador.modelo.Passo;
import br.edu.testador.modelo.Regra;
import br.edu.testador.modelo.Suite;
import br.edu.testador.resultado.Estatisticas;
import br.edu.testador.resultado.ResultadoPasso;
import br.edu.testador.resultado.Verificacao;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;

import java.io.IOException;
import java.net.ConnectException;
import java.net.http.HttpTimeoutException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class ExecutorDePasso {

    private final ClienteHttp clienteHttp;
    private final Interpolador interpolador;
    private final VerificadorDeRespostas verificador;
    private final ObjectMapper json = new ObjectMapper();

    public ExecutorDePasso(ClienteHttp clienteHttp, Interpolador interpolador, VerificadorDeRespostas verificador) {
        this.clienteHttp = clienteHttp;
        this.interpolador = interpolador;
        this.verificador = verificador;
    }

    public ResultadoPasso executar(Passo passo, Suite suite, Contexto contexto) {
        String nome = passo.nome() != null ? passo.nome() : passo.metodo() + " " + passo.caminho();
        String url = null;
        try {
            url = montarUrl(suite.urlBase(), interpolador.texto(passo.caminho(), contexto));
            Map<String, String> cabecalhos = montarCabecalhos(passo, suite, contexto);
            String corpo = montarCorpo(passo, contexto, cabecalhos);
            Espera espera = interpolarEspera(passo.espera(), contexto);

            ResultadoPasso resultado = passo.repetir() == 1
                    ? executarUmaVez(nome, passo, espera, suite, url, cabecalhos, corpo, contexto)
                    : executarRepetido(nome, passo, espera, suite, url, cabecalhos, corpo, contexto);

            if (passo.pausaMs() > 0) {
                Thread.sleep(passo.pausaMs());
            }
            return resultado;
        } catch (VariavelNaoDefinidaException e) {
            return erro(nome, passo, url, e.getMessage());
        } catch (HttpTimeoutException e) {
            return erro(nome, passo, url, "Tempo limite de " + suite.tempoLimiteMs() + " ms excedido.");
        } catch (ConnectException e) {
            return erro(nome, passo, url, "Não foi possível conectar em " + url + ". A API está no ar?");
        } catch (IOException e) {
            return erro(nome, passo, url, "Falha de comunicação: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return erro(nome, passo, url, "Execução interrompida.");
        } catch (IllegalArgumentException e) {
            return erro(nome, passo, url, "Requisição inválida: " + e.getMessage());
        }
    }

    private ResultadoPasso executarUmaVez(String nome, Passo passo, Espera espera, Suite suite, String url,
                                          Map<String, String> cabecalhos, String corpo, Contexto contexto)
            throws IOException, InterruptedException {
        Resposta resposta = clienteHttp.enviar(passo.metodo(), url, cabecalhos, corpo, suite.tempoLimiteMs());
        List<Verificacao> verificacoes = new ArrayList<>(
                verificador.verificar(espera, resposta, suite.verificarVazamento()));
        verificacoes.addAll(capturar(passo.capturar(), resposta, contexto));
        return new ResultadoPasso(nome, passo.metodo(), url, resposta.status(), resposta.duracaoMs(), verificacoes,
                null, null);
    }

    private ResultadoPasso executarRepetido(String nome, Passo passo, Espera espera, Suite suite, String url,
                                            Map<String, String> cabecalhos, String corpo, Contexto contexto)
            throws InterruptedException {
        List<Long> duracoes = Collections.synchronizedList(new ArrayList<>());
        List<String> problemas = Collections.synchronizedList(new ArrayList<>());
        List<Resposta> respostas = Collections.synchronizedList(new ArrayList<>());

        long inicio = System.currentTimeMillis();
        try (ExecutorService executor = Executors.newFixedThreadPool(passo.concorrencia())) {
            List<Future<?>> tarefas = new ArrayList<>();
            for (int i = 0; i < passo.repetir(); i++) {
                tarefas.add(executor.submit(() -> {
                    try {
                        Resposta resposta = clienteHttp.enviar(passo.metodo(), url, cabecalhos, corpo,
                                suite.tempoLimiteMs());
                        duracoes.add(resposta.duracaoMs());
                        respostas.add(resposta);
                        verificador.verificar(espera, resposta, suite.verificarVazamento()).stream()
                                .filter(v -> !v.aprovada())
                                .findFirst()
                                .ifPresent(v -> problemas.add(v.descricao() + ": " + v.detalhe()));
                    } catch (IOException e) {
                        problemas.add("falha de comunicação: " + e.getMessage());
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }));
            }
            for (Future<?> tarefa : tarefas) {
                try {
                    tarefa.get();
                } catch (ExecutionException e) {
                    problemas.add("erro inesperado: " + e.getCause().getMessage());
                }
            }
        }
        long duracaoTotal = System.currentTimeMillis() - inicio;

        Estatisticas estatisticas = Estatisticas.calcular(duracoes, problemas.size());
        List<Verificacao> verificacoes = new ArrayList<>();
        String descricao = passo.repetir() + " requisições atendem às expectativas";
        verificacoes.add(problemas.isEmpty()
                ? Verificacao.ok(descricao)
                : Verificacao.falha(descricao, problemas.size() + " falharam. Primeira: " + problemas.getFirst()));

        if (espera.p95MaximoMs() != null) {
            String descricaoP95 = "p95 até " + espera.p95MaximoMs() + " ms";
            verificacoes.add(estatisticas.p95Ms() <= espera.p95MaximoMs()
                    ? Verificacao.ok(descricaoP95)
                    : Verificacao.falha(descricaoP95, "p95 medido: " + estatisticas.p95Ms() + " ms"));
        }

        Integer status = null;
        if (!respostas.isEmpty()) {
            Resposta primeira = respostas.getFirst();
            status = primeira.status();
            verificacoes.addAll(capturar(passo.capturar(), primeira, contexto));
        }
        return new ResultadoPasso(nome, passo.metodo(), url, status, duracaoTotal, verificacoes, null, estatisticas);
    }

    private List<Verificacao> capturar(Map<String, String> capturas, Resposta resposta, Contexto contexto) {
        List<Verificacao> verificacoes = new ArrayList<>();
        for (Map.Entry<String, String> captura : capturas.entrySet()) {
            String descricao = "capturar " + captura.getKey() + " de " + captura.getValue();
            try {
                Object valor = JsonPath.read(resposta.corpo(), captura.getValue());
                contexto.definir(captura.getKey(), valor);
                verificacoes.add(Verificacao.ok(descricao));
            } catch (RuntimeException e) {
                verificacoes.add(Verificacao.falha(descricao, "valor não encontrado na resposta"));
            }
        }
        return verificacoes;
    }

    private Espera interpolarEspera(Espera espera, Contexto contexto) {
        List<Regra> regras = new ArrayList<>();
        for (Regra r : espera.corpo()) {
            regras.add(new Regra(r.caminho(), interpolador.interpolar(r.igual(), contexto),
                    interpolador.interpolar(r.diferente(), contexto), r.existe(),
                    interpolador.interpolar(r.contem(), contexto), r.maiorQue(), r.menorQue(), r.tamanho(), r.tipo(),
                    interpolador.texto(r.regex(), contexto)));
        }
        Map<String, String> cabecalhos = new LinkedHashMap<>();
        espera.cabecalhos().forEach((k, v) -> cabecalhos.put(k, interpolador.texto(v, contexto)));
        return new Espera(espera.status(), espera.tempoMaximoMs(), espera.p95MaximoMs(),
                interpolador.texto(espera.corpoContem(), contexto), interpolador.texto(espera.corpoNaoContem(), contexto),
                cabecalhos, regras);
    }

    private Map<String, String> montarCabecalhos(Passo passo, Suite suite, Contexto contexto) {
        Map<String, String> cabecalhos = new LinkedHashMap<>();
        suite.cabecalhos().forEach((k, v) -> cabecalhos.put(k, interpolador.texto(v, contexto)));
        passo.cabecalhos().forEach((k, v) -> cabecalhos.put(k, interpolador.texto(v, contexto)));
        if (passo.token() != null && !passo.token().isBlank()) {
            cabecalhos.put("Authorization", "Bearer " + interpolador.texto(passo.token(), contexto));
        }
        return cabecalhos;
    }

    private String montarCorpo(Passo passo, Contexto contexto, Map<String, String> cabecalhos) {
        if (passo.corpo() == null) {
            return null;
        }
        Object corpo = interpolador.interpolar(passo.corpo(), contexto);
        if (corpo instanceof String texto) {
            return texto;
        }
        boolean temContentType = cabecalhos.keySet().stream().anyMatch(k -> k.equalsIgnoreCase("Content-Type"));
        if (!temContentType) {
            cabecalhos.put("Content-Type", "application/json");
        }
        try {
            return json.writeValueAsString(corpo);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("não foi possível converter o corpo para JSON");
        }
    }

    static String montarUrl(String urlBase, String caminho) {
        if (caminho == null || caminho.isBlank()) {
            throw new IllegalArgumentException("o passo não tem 'caminho'");
        }
        if (caminho.startsWith("http://") || caminho.startsWith("https://")) {
            return caminho;
        }
        if (urlBase == null || urlBase.isBlank()) {
            throw new IllegalArgumentException("defina 'urlBase' na suíte ou use --url-base");
        }
        String base = urlBase.endsWith("/") ? urlBase.substring(0, urlBase.length() - 1) : urlBase;
        return base + (caminho.startsWith("/") ? caminho : "/" + caminho);
    }

    private ResultadoPasso erro(String nome, Passo passo, String url, String mensagem) {
        return new ResultadoPasso(nome, passo.metodo(), url, null, 0, List.of(), mensagem, null);
    }
}
