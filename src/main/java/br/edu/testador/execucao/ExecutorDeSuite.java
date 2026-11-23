package br.edu.testador.execucao;

import br.edu.testador.modelo.Cenario;
import br.edu.testador.modelo.Passo;
import br.edu.testador.modelo.Suite;
import br.edu.testador.resultado.ResultadoCenario;
import br.edu.testador.resultado.ResultadoPasso;
import br.edu.testador.resultado.ResultadoSuite;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class ExecutorDeSuite {

    private final ExecutorDePasso executorDePasso;
    private final VarreduraDeSeguranca varredura;
    private final Interpolador interpolador;

    public ExecutorDeSuite(ExecutorDePasso executorDePasso, VarreduraDeSeguranca varredura, Interpolador interpolador) {
        this.executorDePasso = executorDePasso;
        this.varredura = varredura;
        this.interpolador = interpolador;
    }

    public ResultadoSuite executar(Suite suite, String arquivo, Map<String, String> variaveisExtras,
                                   List<String> tags, boolean pararNaFalha, Consumer<ResultadoPasso> aoTerminarPasso) {
        long inicio = System.currentTimeMillis();
        Contexto contexto = new Contexto();
        List<ResultadoCenario> cenarios = new ArrayList<>();

        String erroVariaveis = carregarVariaveis(suite, variaveisExtras, contexto);
        if (erroVariaveis != null) {
            ResultadoPasso falha = new ResultadoPasso("Carregar variáveis", "-", null, null, 0, List.of(),
                    erroVariaveis, null);
            aoTerminarPasso.accept(falha);
            cenarios.add(new ResultadoCenario("Preparação", List.of(falha)));
            return new ResultadoSuite(nomeDa(suite, arquivo), arquivo, cenarios, System.currentTimeMillis() - inicio);
        }

        boolean interromper = false;
        for (Cenario cenario : suite.cenarios()) {
            if (!cenario.possuiAlgumaTag(tags)) {
                continue;
            }
            List<ResultadoPasso> passos = new ArrayList<>();
            for (Passo passo : cenario.passos()) {
                ResultadoPasso resultado = executorDePasso.executar(passo, suite, contexto);
                passos.add(resultado);
                aoTerminarPasso.accept(resultado);
                if (!resultado.aprovado()) {
                    interromper = pararNaFalha;
                    break;
                }
            }
            cenarios.add(new ResultadoCenario(cenario.nome(), passos));
            if (interromper) {
                break;
            }
        }

        if (!interromper && suite.seguranca() != null) {
            ResultadoCenario resultadoVarredura = varredura.executar(suite, contexto);
            resultadoVarredura.passos().forEach(aoTerminarPasso);
            cenarios.add(resultadoVarredura);
        }

        return new ResultadoSuite(nomeDa(suite, arquivo), arquivo, cenarios, System.currentTimeMillis() - inicio);
    }

    private String carregarVariaveis(Suite suite, Map<String, String> extras, Contexto contexto) {
        extras.forEach(contexto::definir);
        try {
            for (Map.Entry<String, Object> variavel : suite.variaveis().entrySet()) {
                if (!extras.containsKey(variavel.getKey())) {
                    contexto.definir(variavel.getKey(), interpolador.interpolar(variavel.getValue(), contexto));
                }
            }
            return null;
        } catch (VariavelNaoDefinidaException e) {
            return e.getMessage();
        }
    }

    private String nomeDa(Suite suite, String arquivo) {
        return suite.nome() == null || suite.nome().isBlank() ? arquivo : suite.nome();
    }
}
