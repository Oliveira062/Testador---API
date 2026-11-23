package br.edu.testador.execucao;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Interpolador {

    private static final Pattern VARIAVEL = Pattern.compile("\\$\\{([^}]+)}");

    private final Function<String, String> ambiente;

    public Interpolador() {
        this(System::getenv);
    }

    public Interpolador(Function<String, String> ambiente) {
        this.ambiente = ambiente;
    }

    public Object interpolar(Object valor, Contexto contexto) {
        if (valor instanceof String texto) {
            return interpolarTexto(texto, contexto);
        }
        if (valor instanceof Map<?, ?> mapa) {
            Map<String, Object> resultado = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entrada : mapa.entrySet()) {
                resultado.put(String.valueOf(entrada.getKey()), interpolar(entrada.getValue(), contexto));
            }
            return resultado;
        }
        if (valor instanceof List<?> lista) {
            List<Object> resultado = new ArrayList<>();
            for (Object item : lista) {
                resultado.add(interpolar(item, contexto));
            }
            return resultado;
        }
        return valor;
    }

    public String texto(String valor, Contexto contexto) {
        Object resultado = interpolar(valor, contexto);
        return resultado == null ? null : String.valueOf(resultado);
    }

    private Object interpolarTexto(String texto, Contexto contexto) {
        Matcher unico = VARIAVEL.matcher(texto);
        if (unico.matches()) {
            return resolver(unico.group(1).trim(), contexto);
        }
        Matcher matcher = VARIAVEL.matcher(texto);
        StringBuilder saida = new StringBuilder();
        while (matcher.find()) {
            Object valor = resolver(matcher.group(1).trim(), contexto);
            matcher.appendReplacement(saida, Matcher.quoteReplacement(String.valueOf(valor)));
        }
        matcher.appendTail(saida);
        return saida.toString();
    }

    private Object resolver(String nome, Contexto contexto) {
        if (nome.equals("aleatorio")) {
            return UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        }
        if (nome.equals("uuid")) {
            return UUID.randomUUID().toString();
        }
        if (nome.equals("agora")) {
            return Instant.now().toString();
        }
        if (nome.equals("timestamp")) {
            return System.currentTimeMillis();
        }
        if (nome.startsWith("env.")) {
            String variavel = nome.substring(4);
            String valor = ambiente.apply(variavel);
            if (valor == null) {
                throw new VariavelNaoDefinidaException(nome);
            }
            return valor;
        }
        if (!contexto.possui(nome)) {
            throw new VariavelNaoDefinidaException(nome);
        }
        return contexto.obter(nome);
    }
}
