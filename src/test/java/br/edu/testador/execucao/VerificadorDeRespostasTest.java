package br.edu.testador.execucao;

import br.edu.testador.modelo.Espera;
import br.edu.testador.modelo.Regra;
import br.edu.testador.resultado.Verificacao;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VerificadorDeRespostasTest {

    private static final String CORPO = """
            {"id": 7, "nome": "Coxinha", "preco": 6.00, "ativo": true,
             "tags": ["salgado", "frito"], "itens": [{"qtd": 2}, {"qtd": 3}]}
            """;

    private final VerificadorDeRespostas verificador = new VerificadorDeRespostas();

    private Regra regra(String caminho) {
        return new Regra(caminho, null, null, null, null, null, null, null, null, null);
    }

    private List<Verificacao> verificar(Espera espera, int status, String corpo) {
        return verificador.verificar(espera, new Resposta(status, Map.of(), corpo, 10), true);
    }

    private boolean todasAprovadas(List<Verificacao> verificacoes) {
        return verificacoes.stream().allMatch(Verificacao::aprovada);
    }

    @Test
    void conferePorStatusETempo() {
        Espera espera = new Espera(List.of(200, 201), 50L, null, null, null, null, null);
        assertTrue(todasAprovadas(verificar(espera, 201, CORPO)));
        assertFalse(todasAprovadas(verificar(espera, 500, CORPO)));
    }

    @Test
    void comparaNumerosSemSeImportarComCasasDecimais() {
        Regra preco = new Regra("$.preco", 6, null, null, null, null, null, null, null, null);
        Espera espera = new Espera(null, null, null, null, null, null, List.of(preco));
        assertTrue(todasAprovadas(verificar(espera, 200, CORPO)));
    }

    @Test
    void aplicaRegrasDeTipoTamanhoContemERegex() {
        List<Regra> regras = List.of(
                new Regra("$.tags", null, null, null, "frito", null, null, 2, "lista", null),
                new Regra("$.nome", null, "Pastel", null, null, null, null, null, "texto", "^Cox"),
                new Regra("$.itens[*].qtd", null, null, null, 3, null, null, null, null, null),
                new Regra("$.id", null, null, null, null, 5.0, 10.0, null, "numero", null),
                new Regra("$.ativo", true, null, null, null, null, null, null, "booleano", null));
        Espera espera = new Espera(null, null, null, null, null, null, regras);
        List<Verificacao> resultado = verificar(espera, 200, CORPO);
        assertTrue(todasAprovadas(resultado), resultado.toString());
    }

    @Test
    void detectaCampoAusenteEPresente() {
        Regra senhaNaoExiste = new Regra("$.senha", null, null, false, null, null, null, null, null, null);
        Regra idExiste = new Regra("$.id", null, null, true, null, null, null, null, null, null);
        Espera espera = new Espera(null, null, null, null, null, null, List.of(senhaNaoExiste, idExiste));
        assertTrue(todasAprovadas(verificar(espera, 200, CORPO)));

        Espera inverso = new Espera(null, null, null, null, null, null, List.of(regra("$.inexistente")));
        assertTrue(todasAprovadas(verificar(inverso, 200, CORPO)));
    }

    @Test
    void reprovaQuandoACondicaoNaoBate() {
        Regra errada = new Regra("$.nome", "Pastel", null, null, null, null, null, null, null, null);
        List<Verificacao> resultado = verificar(new Espera(null, null, null, null, null, null, List.of(errada)), 200, CORPO);
        assertEquals(1, resultado.size());
        assertFalse(resultado.getFirst().aprovada());
        assertTrue(resultado.getFirst().detalhe().contains("Coxinha"));
    }

    @Test
    void acusaVazamentoDeStackTraceEmErro() {
        String vazado = "java.lang.NullPointerException: x\n\tat br.com.App.metodo(App.java:42)";
        List<Verificacao> resultado = verificar(Espera.padrao(), 500, vazado);
        assertFalse(todasAprovadas(resultado));
        assertTrue(todasAprovadas(verificar(Espera.padrao(), 500, "{\"mensagem\": \"Erro interno\"}")));
    }

    @Test
    void conferePresencaDeCabecalho() {
        Espera espera = new Espera(null, null, null, null, null, Map.of("content-type", "json"), null);
        Resposta resposta = new Resposta(200, Map.of("Content-Type", List.of("application/json")), "{}", 5);
        assertTrue(todasAprovadas(verificador.verificar(espera, resposta, true)));
    }
}
