# Relatório de Evidências — OCI Queue Java

**Atividade:** Reimplementação do exemplo de Filas da OCI em Java (sem Node.js).  
**Data dos testes:** 22 de outubro de 2026.  
**Fila:** `fila-trabalho-java` — OCI US West (Phoenix).  
**Ambiente:** Java 21.0.2, Maven 3.10.0, Windows, VS Code.  
**Modo de processamento de e-mail:** `LOG` (simulado, sem envio real).

## Evidências anexadas

| Nº | Arquivo | Descrição e correspondência com o requisito |
|---|---|---|
| 01 | [`evidencias/01-fila-criada.png`](evidencias/01-fila-criada.png) | **Criação e configuração da fila OCI.** Fila ativa; nome, timeout de visibilidade, retenção, tentativas, endpoint. |
| 02 | [`evidencias/02-compilacao-testes.png`](evidencias/02-compilacao-testes.png) | **Compilação e testes automatizados.** Maven: 3 testes, 0 falhas, 0 erros, BUILD SUCCESS. |
| 03 | [`evidencias/03-produtor-sem-canal.png`](evidencias/03-produtor-sem-canal.png) | **Publicação sem canal.** Execução Java e PutMessages com ID confirmado pela OCI. |
| 04 | [`evidencias/04-consumidor-sem-canal.png`](evidencias/04-consumidor-sem-canal.png) | **Consumo e exclusão.** Mesmo ID recuperado, processamento em LOG e DeleteMessage. |
| 05 | [`evidencias/05-producao-consumo-canal.png`](evidencias/05-producao-consumo-canal.png) | **Publicação e consumo por canal.** Canal emails; mesmo identificador na produção e no consumo. |
| 06 | [`evidencias/06-falha-processamento.png`](evidencias/06-falha-processamento.png) | **Tratamento de falha.** Payload inválido rejeitado; tentativa 1; reprogramação de visibilidade. |
| 07 | [`evidencias/07-reentrega-segunda-tentativa.png`](evidencias/07-reentrega-segunda-tentativa.png) | **Reentrega após falha.** Mesmo ID da mensagem inválida recebido na tentativa 2. |

## Conclusão técnica

O fluxo demonstrou a publicação e o consumo de mensagens pela OCI Queue em Java, inclusive com identificação consistente das mensagens, uso de canal, exclusão após processamento e reentrega após erro. Os três testes automatizados do modelo foram aprovados. O código está organizado por funções e contém tratamento de erros e comentários explicativos.

### Precisão das evidências

- As capturas 3, 4 e 5 comprovam **operações reais na OCI**; as linhas `[SIMULAÇÃO]` indicam apenas que não houve envio real de e-mail.
- As capturas 6 e 7 comprovam o tratamento da falha e o retorno da mesma mensagem com `tentativa=2`. Não demonstram seu ingresso final na DLQ.
- A captura 2 é da versão usada nos testes, antes do pequeno ajuste posterior de dependência de logging no `pom.xml`. Caso use o ZIP revisado, recomenda-se executar `mvn clean test` mais uma vez.
- A árvore de diretórios e os comentários podem ser verificados no código entregue. Se o professor solicitar uma captura específica desses critérios, tirar print do explorador do VS Code com `src/main/java` aberto e o arquivo `MessageConsumer.java` visível.
- Os caracteres acentuados incorretos nas capturas foram causados pela codificação do terminal; não afetam as operações com a OCI.

## Mapeamento para a rubrica da atividade

- **Preservar a lógica:** produtor, consumidor, JSON de solicitação de e-mail, canal, tratamento de erro, reentrega e confirmação da mensagem.
- **Organização:** `config`, `producer`, `consumer`, `model`, `email`, além de testes separados em `src/test/java`.
- **Clareza e comentários:** métodos de responsabilidade delimitada e comentários sobre serialização, reentrega e exclusão após sucesso.
- **Boas práticas:** credenciais externas, validações, tratamento de exceções, testes JUnit e uso do SDK oficial.

**Não se afirma execução de envio SMTP real, de serviço Windows, nem teste final da DLQ.**
