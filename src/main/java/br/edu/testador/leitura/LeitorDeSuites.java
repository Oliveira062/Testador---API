package br.edu.testador.leitura;

import br.edu.testador.modelo.Cenario;
import br.edu.testador.modelo.Passo;
import br.edu.testador.modelo.Suite;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

public class LeitorDeSuites {

    private static final Set<String> METODOS = Set.of("GET", "POST", "PUT", "PATCH", "DELETE", "HEAD", "OPTIONS");

    private final ObjectMapper yaml = new ObjectMapper(new YAMLFactory())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);
    private final ObjectMapper json = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);

    public List<Path> localizarArquivos(List<String> entradas) throws IOException {
        List<Path> arquivos = new ArrayList<>();
        for (String entrada : entradas) {
            Path caminho = Path.of(entrada);
            if (Files.isDirectory(caminho)) {
                try (Stream<Path> conteudo = Files.list(caminho)) {
                    conteudo.filter(LeitorDeSuites::ehArquivoDeSuite).sorted().forEach(arquivos::add);
                }
            } else if (Files.isRegularFile(caminho)) {
                arquivos.add(caminho);
            } else {
                throw new ArquivoInvalidoException("Arquivo ou pasta não encontrado: " + entrada);
            }
        }
        if (arquivos.isEmpty()) {
            throw new ArquivoInvalidoException("Nenhum arquivo .yaml, .yml ou .json encontrado.");
        }
        return arquivos;
    }

    public Suite ler(Path arquivo) {
        Suite suite;
        try {
            ObjectMapper leitor = arquivo.toString().toLowerCase().endsWith(".json") ? json : yaml;
            suite = leitor.readValue(arquivo.toFile(), Suite.class);
        } catch (IOException e) {
            throw new ArquivoInvalidoException("Erro ao ler " + arquivo.getFileName() + ": " + resumir(e.getMessage()));
        }
        validar(suite, arquivo);
        return suite;
    }

    private void validar(Suite suite, Path arquivo) {
        String nomeArquivo = arquivo.getFileName().toString();
        if (suite.cenarios().isEmpty() && suite.seguranca() == null) {
            throw new ArquivoInvalidoException(nomeArquivo + ": a suíte precisa ter 'cenarios' ou 'seguranca'.");
        }
        for (int i = 0; i < suite.cenarios().size(); i++) {
            Cenario cenario = suite.cenarios().get(i);
            String onde = nomeArquivo + ", cenário " + (i + 1);
            if (cenario.nome() == null || cenario.nome().isBlank()) {
                throw new ArquivoInvalidoException(onde + ": informe o 'nome' do cenário.");
            }
            if (cenario.passos().isEmpty()) {
                throw new ArquivoInvalidoException(onde + " (" + cenario.nome() + "): o cenário não tem 'passos'.");
            }
            for (int j = 0; j < cenario.passos().size(); j++) {
                Passo passo = cenario.passos().get(j);
                String ondePasso = onde + ", passo " + (j + 1);
                if (passo.caminho() == null || passo.caminho().isBlank()) {
                    throw new ArquivoInvalidoException(ondePasso + ": informe o 'caminho', ex.: /api/produtos.");
                }
                if (!METODOS.contains(passo.metodo())) {
                    throw new ArquivoInvalidoException(ondePasso + ": método '" + passo.metodo() + "' desconhecido.");
                }
            }
        }
    }

    private static boolean ehArquivoDeSuite(Path caminho) {
        String nome = caminho.getFileName().toString().toLowerCase();
        return nome.endsWith(".yaml") || nome.endsWith(".yml") || nome.endsWith(".json");
    }

    private static String resumir(String mensagem) {
        if (mensagem == null) {
            return "formato inválido";
        }
        int quebra = mensagem.indexOf("\n at [Source");
        String texto = quebra > 0 ? mensagem.substring(0, quebra) : mensagem;
        return texto.replace("Unrecognized field", "Campo desconhecido").trim();
    }
}
