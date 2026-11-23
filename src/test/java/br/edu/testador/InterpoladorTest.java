package br.edu.testador;

import br.edu.testador.execucao.Contexto;
import br.edu.testador.execucao.Interpolador;
import br.edu.testador.execucao.VariavelNaoDefinidaException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InterpoladorTest {

    private final Interpolador interpolador = new Interpolador(nome -> nome.equals("SENHA") ? "segredo" : null);

    @Test
    void substituiVariavelNoMeioDoTexto() {
        Contexto contexto = new Contexto();
        contexto.definir("id", 42);
        assertEquals("/api/pedidos/42/itens", interpolador.texto("/api/pedidos/${id}/itens", contexto));
    }

    @Test
    void mantemOTipoQuandoOTextoEhSoAVariavel() {
        Contexto contexto = new Contexto();
        contexto.definir("id", 42);
        assertEquals(42, interpolador.interpolar("${id}", contexto));
    }

    @Test
    void percorreMapasEListas() {
        Contexto contexto = new Contexto();
        contexto.definir("nome", "Ana");
        Object resultado = interpolador.interpolar(Map.of("cliente", List.of("${nome}", "fixo")), contexto);
        assertEquals(Map.of("cliente", List.of("Ana", "fixo")), resultado);
    }

    @Test
    void leVariavelDeAmbiente() {
        assertEquals("segredo", interpolador.texto("${env.SENHA}", new Contexto()));
    }

    @Test
    void geraValoresAleatoriosDiferentes() {
        Contexto contexto = new Contexto();
        assertNotEquals(interpolador.texto("${aleatorio}", contexto), interpolador.texto("${aleatorio}", contexto));
    }

    @Test
    void avisaQuandoAVariavelNaoExiste() {
        assertThrows(VariavelNaoDefinidaException.class, () -> interpolador.texto("${token}", new Contexto()));
        assertThrows(VariavelNaoDefinidaException.class, () -> interpolador.texto("${env.NAO_EXISTE}", new Contexto()));
    }
}
