package br.edu.testador.relatorio;

import br.edu.testador.resultado.ResultadoSuite;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public interface Relatorio {

    Path gerar(List<ResultadoSuite> resultados, Path pasta) throws IOException;
}
