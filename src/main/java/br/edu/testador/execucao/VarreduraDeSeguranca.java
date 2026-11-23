package br.edu.testador.execucao;

import br.edu.testador.modelo.Seguranca;
import br.edu.testador.modelo.Suite;
import br.edu.testador.modelo.VerificacaoCabecalhos;
import br.edu.testador.resultado.ResultadoCenario;
import br.edu.testador.resultado.ResultadoPasso;
import br.edu.testador.resultado.Verificacao;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class VarreduraDeSeguranca {

    private static final String TOKEN_MALFORMADO = "isto.nao.e-um-token";

    private final ClienteHttp clienteHttp;
    private final Interpolador interpolador;

    public VarreduraDeSeguranca(ClienteHttp clienteHttp, Interpolador interpolador) {
        this.clienteHttp = clienteHttp;
        this.interpolador = interpolador;
    }

    public ResultadoCenario executar(Suite suite, Contexto contexto) {
        Seguranca seguranca = suite.seguranca();
        List<ResultadoPasso> passos = new ArrayList<>();

        Map<String, String> tentativas = new LinkedHashMap<>();
        tentativas.put("sem token", null);
        tentativas.put("token malformado", TOKEN_MALFORMADO);
        tentativas.put("token com algoritmo none", tokenSemAssinatura());

        for (String rota : seguranca.rotasProtegidas()) {
            String[] partes = rota.trim().split("\\s+", 2);
            String metodo = partes.length == 2 ? partes[0].toUpperCase() : "GET";
            String caminho = partes.length == 2 ? partes[1] : partes[0];
            for (Map.Entry<String, String> tentativa : tentativas.entrySet()) {
                passos.add(testarRota(suite, contexto, metodo, caminho, tentativa.getKey(), tentativa.getValue(),
                        seguranca.statusAceitos()));
            }
        }

        if (seguranca.cabecalhos() != null) {
            passos.add(testarCabecalhos(suite, contexto, seguranca.cabecalhos()));
        }
        return new ResultadoCenario("Varredura de segurança", passos);
    }

    private ResultadoPasso testarRota(Suite suite, Contexto contexto, String metodo, String caminho, String descricao,
                                      String token, List<Integer> aceitos) {
        String nome = metodo + " " + caminho + " (" + descricao + ")";
        String url = null;
        try {
            url = ExecutorDePasso.montarUrl(suite.urlBase(), interpolador.texto(caminho, contexto));
            Map<String, String> cabecalhos = new LinkedHashMap<>();
            if (token != null) {
                cabecalhos.put("Authorization", "Bearer " + token);
            }
            String corpo = null;
            if (!metodo.equals("GET") && !metodo.equals("DELETE")) {
                cabecalhos.put("Content-Type", "application/json");
                corpo = "{}";
            }
            Resposta resposta = clienteHttp.enviar(metodo, url, cabecalhos, corpo, suite.tempoLimiteMs());
            String regra = "bloqueado com status em " + aceitos;
            Verificacao verificacao = aceitos.contains(resposta.status())
                    ? Verificacao.ok(regra)
                    : Verificacao.falha(regra, "a rota respondeu " + resposta.status() + " e deveria recusar o acesso");
            return new ResultadoPasso(nome, metodo, url, resposta.status(), resposta.duracaoMs(), List.of(verificacao),
                    null, null);
        } catch (IOException | RuntimeException e) {
            return new ResultadoPasso(nome, metodo, url, null, 0, List.of(), "Falha ao testar: " + e.getMessage(), null);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new ResultadoPasso(nome, metodo, url, null, 0, List.of(), "Execução interrompida.", null);
        }
    }

    private ResultadoPasso testarCabecalhos(Suite suite, Contexto contexto, VerificacaoCabecalhos config) {
        String nome = "Cabeçalhos de segurança em GET " + config.caminho();
        String url = null;
        try {
            url = ExecutorDePasso.montarUrl(suite.urlBase(), interpolador.texto(config.caminho(), contexto));
            Resposta resposta = clienteHttp.enviar("GET", url, Map.of(), null, suite.tempoLimiteMs());
            List<Verificacao> verificacoes = new ArrayList<>();
            for (String cabecalho : config.exigir()) {
                verificacoes.add(resposta.cabecalho(cabecalho) != null
                        ? Verificacao.ok("cabeçalho " + cabecalho + " presente")
                        : Verificacao.falha("cabeçalho " + cabecalho + " presente", "ausente na resposta"));
            }
            return new ResultadoPasso(nome, "GET", url, resposta.status(), resposta.duracaoMs(), verificacoes, null, null);
        } catch (IOException | RuntimeException e) {
            return new ResultadoPasso(nome, "GET", url, null, 0, List.of(), "Falha ao testar: " + e.getMessage(), null);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new ResultadoPasso(nome, "GET", url, null, 0, List.of(), "Execução interrompida.", null);
        }
    }

    private String tokenSemAssinatura() {
        Base64.Encoder codificador = Base64.getUrlEncoder().withoutPadding();
        String cabecalho = codificador.encodeToString("{\"alg\":\"none\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));
        String conteudo = codificador.encodeToString(
                "{\"sub\":\"1\",\"perfil\":\"ADMIN\",\"role\":\"admin\",\"exp\":4102444800}".getBytes(StandardCharsets.UTF_8));
        return cabecalho + "." + conteudo + ".";
    }
}
