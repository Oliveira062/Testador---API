package br.edu.testador.execucao;

import br.edu.testador.modelo.Espera;
import br.edu.testador.modelo.Regra;
import br.edu.testador.resultado.Verificacao;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.InvalidJsonException;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class VerificadorDeRespostas {

    private static final Pattern SINAIS_DE_VAZAMENTO = Pattern.compile(
            "(\\bat [a-zA-Z0-9_.$]+\\([A-Za-z0-9_]+\\.java:\\d+\\))|(Exception:)|(Traceback \\(most recent call last\\))"
                    + "|(SQLSTATE)|(org\\.hibernate\\.)|(psycopg2\\.)|(ORA-\\d{5})");

    private static final int LIMITE_TRECHO = 200;

    public List<Verificacao> verificar(Espera espera, Resposta resposta, boolean verificarVazamento) {
        List<Verificacao> verificacoes = new ArrayList<>();

        if (!espera.status().isEmpty()) {
            String descricao = "status " + juntar(espera.status());
            verificacoes.add(espera.status().contains(resposta.status())
                    ? Verificacao.ok(descricao)
                    : Verificacao.falha(descricao, "recebido " + resposta.status() + trechoDoCorpo(resposta)));
        }

        if (espera.tempoMaximoMs() != null) {
            String descricao = "tempo até " + espera.tempoMaximoMs() + " ms";
            verificacoes.add(resposta.duracaoMs() <= espera.tempoMaximoMs()
                    ? Verificacao.ok(descricao)
                    : Verificacao.falha(descricao, "levou " + resposta.duracaoMs() + " ms"));
        }

        String corpo = resposta.corpo() == null ? "" : resposta.corpo();
        if (espera.corpoContem() != null) {
            String descricao = "corpo contém \"" + espera.corpoContem() + "\"";
            verificacoes.add(corpo.contains(espera.corpoContem())
                    ? Verificacao.ok(descricao)
                    : Verificacao.falha(descricao, "texto não encontrado" + trechoDoCorpo(resposta)));
        }
        if (espera.corpoNaoContem() != null) {
            String descricao = "corpo não contém \"" + espera.corpoNaoContem() + "\"";
            verificacoes.add(!corpo.contains(espera.corpoNaoContem())
                    ? Verificacao.ok(descricao)
                    : Verificacao.falha(descricao, "texto encontrado"));
        }

        for (Map.Entry<String, String> esperado : espera.cabecalhos().entrySet()) {
            String descricao = "cabeçalho " + esperado.getKey() + " contém \"" + esperado.getValue() + "\"";
            String recebido = resposta.cabecalho(esperado.getKey());
            verificacoes.add(recebido != null && recebido.contains(esperado.getValue())
                    ? Verificacao.ok(descricao)
                    : Verificacao.falha(descricao, recebido == null ? "cabeçalho ausente" : "recebido: " + recebido));
        }

        if (!espera.corpo().isEmpty()) {
            verificarRegrasJson(espera.corpo(), corpo, verificacoes);
        }

        if (verificarVazamento && resposta.status() >= 400 && SINAIS_DE_VAZAMENTO.matcher(corpo).find()) {
            verificacoes.add(Verificacao.falha("resposta de erro sem detalhes internos",
                    "a resposta expõe stack trace ou mensagem interna" + trechoDoCorpo(resposta)));
        }

        return verificacoes;
    }

    private void verificarRegrasJson(List<Regra> regras, String corpo, List<Verificacao> verificacoes) {
        DocumentContext documento;
        try {
            documento = JsonPath.parse(corpo);
        } catch (InvalidJsonException | IllegalArgumentException e) {
            verificacoes.add(Verificacao.falha("corpo em JSON", "a resposta não é um JSON válido"));
            return;
        }
        for (Regra regra : regras) {
            verificacoes.addAll(aplicarRegra(regra, documento));
        }
    }

    List<Verificacao> aplicarRegra(Regra regra, DocumentContext documento) {
        List<Verificacao> resultado = new ArrayList<>();
        String caminho = regra.caminho();
        if (caminho == null || caminho.isBlank()) {
            resultado.add(Verificacao.falha("regra sem caminho", "toda regra precisa de 'caminho', ex.: $.id"));
            return resultado;
        }

        Object valor;
        boolean encontrado;
        try {
            valor = documento.read(caminho);
            encontrado = true;
        } catch (PathNotFoundException e) {
            valor = null;
            encontrado = false;
        } catch (RuntimeException e) {
            resultado.add(Verificacao.falha(caminho, "caminho JSONPath inválido: " + e.getMessage()));
            return resultado;
        }

        if (regra.existe() != null) {
            boolean esperado = regra.existe();
            resultado.add(encontrado == esperado
                    ? Verificacao.ok(caminho + (esperado ? " existe" : " não existe"))
                    : Verificacao.falha(caminho + (esperado ? " existe" : " não existe"),
                    esperado ? "campo ausente" : "campo presente com valor " + valor));
        }

        boolean temOutrasRegras = regra.igual() != null || regra.diferente() != null || regra.contem() != null
                || regra.maiorQue() != null || regra.menorQue() != null || regra.tamanho() != null
                || regra.tipo() != null || regra.regex() != null;
        if (!temOutrasRegras) {
            return resultado;
        }
        if (!encontrado) {
            resultado.add(Verificacao.falha(caminho, "campo ausente na resposta"));
            return resultado;
        }

        if (regra.igual() != null) {
            String descricao = caminho + " = " + regra.igual();
            resultado.add(saoIguais(valor, regra.igual())
                    ? Verificacao.ok(descricao)
                    : Verificacao.falha(descricao, "recebido: " + valor));
        }
        if (regra.diferente() != null) {
            String descricao = caminho + " ≠ " + regra.diferente();
            resultado.add(!saoIguais(valor, regra.diferente())
                    ? Verificacao.ok(descricao)
                    : Verificacao.falha(descricao, "recebido exatamente " + valor));
        }
        if (regra.contem() != null) {
            String descricao = caminho + " contém " + regra.contem();
            resultado.add(contem(valor, regra.contem())
                    ? Verificacao.ok(descricao)
                    : Verificacao.falha(descricao, "recebido: " + valor));
        }
        if (regra.maiorQue() != null) {
            String descricao = caminho + " > " + regra.maiorQue();
            BigDecimal numero = comoNumero(valor);
            resultado.add(numero != null && numero.compareTo(BigDecimal.valueOf(regra.maiorQue())) > 0
                    ? Verificacao.ok(descricao)
                    : Verificacao.falha(descricao, "recebido: " + valor));
        }
        if (regra.menorQue() != null) {
            String descricao = caminho + " < " + regra.menorQue();
            BigDecimal numero = comoNumero(valor);
            resultado.add(numero != null && numero.compareTo(BigDecimal.valueOf(regra.menorQue())) < 0
                    ? Verificacao.ok(descricao)
                    : Verificacao.falha(descricao, "recebido: " + valor));
        }
        if (regra.tamanho() != null) {
            String descricao = caminho + " com tamanho " + regra.tamanho();
            Integer tamanho = tamanhoDe(valor);
            resultado.add(tamanho != null && tamanho.equals(regra.tamanho())
                    ? Verificacao.ok(descricao)
                    : Verificacao.falha(descricao, "tamanho recebido: " + tamanho));
        }
        if (regra.tipo() != null) {
            String descricao = caminho + " do tipo " + regra.tipo();
            String tipoReal = tipoDe(valor);
            resultado.add(tipoReal.equalsIgnoreCase(regra.tipo().trim())
                    ? Verificacao.ok(descricao)
                    : Verificacao.falha(descricao, "tipo recebido: " + tipoReal));
        }
        if (regra.regex() != null) {
            String descricao = caminho + " segue o padrão " + regra.regex();
            resultado.add(valor != null && Pattern.compile(regra.regex()).matcher(String.valueOf(valor)).find()
                    ? Verificacao.ok(descricao)
                    : Verificacao.falha(descricao, "recebido: " + valor));
        }
        return resultado;
    }

    static boolean saoIguais(Object recebido, Object esperado) {
        BigDecimal a = comoNumero(recebido);
        BigDecimal b = comoNumero(esperado);
        if (a != null && b != null && (recebido instanceof Number || esperado instanceof Number)) {
            return a.compareTo(b) == 0;
        }
        return String.valueOf(recebido).equals(String.valueOf(esperado));
    }

    private static boolean contem(Object valor, Object procurado) {
        if (valor instanceof Collection<?> colecao) {
            return colecao.stream().anyMatch(item -> saoIguais(item, procurado));
        }
        return valor != null && String.valueOf(valor).contains(String.valueOf(procurado));
    }

    private static BigDecimal comoNumero(Object valor) {
        if (valor instanceof Number numero) {
            return new BigDecimal(numero.toString());
        }
        if (valor instanceof String texto) {
            try {
                return new BigDecimal(texto.trim());
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    private static Integer tamanhoDe(Object valor) {
        if (valor instanceof Collection<?> colecao) {
            return colecao.size();
        }
        if (valor instanceof Map<?, ?> mapa) {
            return mapa.size();
        }
        if (valor instanceof String texto) {
            return texto.length();
        }
        return null;
    }

    private static String tipoDe(Object valor) {
        if (valor == null) {
            return "nulo";
        }
        if (valor instanceof String) {
            return "texto";
        }
        if (valor instanceof Number) {
            return "numero";
        }
        if (valor instanceof Boolean) {
            return "booleano";
        }
        if (valor instanceof Collection<?>) {
            return "lista";
        }
        if (valor instanceof Map<?, ?>) {
            return "objeto";
        }
        return valor.getClass().getSimpleName();
    }

    private static String juntar(List<Integer> status) {
        return status.size() == 1 ? String.valueOf(status.getFirst()) : "em " + status;
    }

    private static String trechoDoCorpo(Resposta resposta) {
        String corpo = resposta.corpo();
        if (corpo == null || corpo.isBlank()) {
            return "";
        }
        String limpo = corpo.replaceAll("\\s+", " ").trim();
        return " | corpo: " + (limpo.length() > LIMITE_TRECHO ? limpo.substring(0, LIMITE_TRECHO) + "..." : limpo);
    }
}
