package br.edu.testador.modelo;

import java.util.List;

public record Seguranca(
        List<String> rotasProtegidas,
        List<Integer> statusAceitos,
        VerificacaoCabecalhos cabecalhos
) {

    public Seguranca {
        rotasProtegidas = rotasProtegidas == null ? List.of() : rotasProtegidas;
        statusAceitos = statusAceitos == null || statusAceitos.isEmpty() ? List.of(401, 403) : statusAceitos;
    }
}
