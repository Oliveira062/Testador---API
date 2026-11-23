# testador-api

Ferramenta de linha de comando para testar qualquer API REST a partir de arquivos YAML ou JSON.
Não depende da linguagem em que a API foi feita: serve para Java, Python, Node, PHP ou o que for.

## Gerar e executar

```bash
mvn package
java -jar target/testador-api.jar exemplos/modelo.yaml
```

Ou use `./testar.sh` (Linux/macOS) ou `testar.bat` (Windows), que geram o jar na primeira vez.

É preciso ter o JDK 21 e o Maven 3.9 ou mais novo instalados.

## O que ela faz

- executa cenários em sequência e guarda valores das respostas (como o token) para os próximos passos
- confere status, tempo de resposta, cabeçalhos e campos do JSON (JSONPath)
- repete requisições em paralelo e mede mínimo, média, p95 e máximo
- faz uma varredura de segurança nas rotas protegidas (sem token, token malformado e token sem assinatura)
- confere cabeçalhos de segurança e acusa respostas de erro que vazam stack trace
- gera `relatorio.html` e `junit.xml` (aceito por GitHub Actions, GitLab e Jenkins)
- devolve código de saída 0, 1 ou 2, para uso em integração contínua

## Opções

```
--url-base URL        substitui a urlBase dos arquivos
--var nome=valor      define variáveis (pode repetir)
--tags a,b            roda só os cenários com essas tags
--relatorios PASTA    pasta dos relatórios (padrão: relatorios)
--parar-na-falha      para no primeiro passo reprovado
--sem-cor             saída sem cores
```
