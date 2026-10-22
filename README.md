# Reimplementação de Filas OCI em Java

## Visão geral

Reimplementação acadêmica do exemplo de **filas da Oracle Cloud Infrastructure (OCI)** originalmente demonstrado em Node.js, utilizando **Java, Maven e o OCI Java SDK**. A solução implementa um produtor e um consumidor de mensagens de solicitação de e-mail. O uso de **Node.js foi eliminado**.

**Estado da validação:** os testes e as operações da fila foram executados na OCI em 22/10/2026. Foram demonstrados envio, recebimento, exclusão após sucesso, canal `emails`, tratamento de mensagem inválida e reentrega (tentativas 1 e 2). As evidências reais estão na pasta [`evidencias/`](evidencias/), indexadas em [`RELATORIO_EVIDENCIAS.md`](RELATORIO_EVIDENCIAS.md).

> **Limite do teste:** o processamento de e-mail foi realizado com `MAIL_MODE=LOG`, ou seja, o e-mail **não foi efetivamente enviado**. As operações com a OCI Queue **foram reais**. O projeto também contém implementação SMTP, opcional, que não foi demonstrada nas evidências. O funcionamento contínuo está implementado nas opções 5/6, mas não há captura de um teste dessa modalidade.

## Lógica preservada do exemplo

```text
Produtor Java -> PutMessages -> OCI Queue -> GetMessages -> Consumidor Java
                                                      |
                                      JSON {"email":"...","msg":"..."}
                                                      |
                                  sucesso: processar -> DeleteMessage
                                  erro: UpdateMessage(visibilityInSeconds=30)
                                        -> reentrega / eventual DLQ
```

- Uma mensagem JSON representa uma solicitação de e-mail, usando os campos `email` e `msg`.
- A aplicação pode publicar na fila sem canal, ou com um `channelId` (`emails`).
- O consumidor recebe até 10 mensagens por consulta e pode filtrar por canal.
- O consumidor só exclui a mensagem após o processamento bem-sucedido.
- Em falha, registra a exceção, atualiza a visibilidade para 30 segundos e permite reentrega.
- Ao atingir o limite de tentativas configurado na OCI, a mensagem pode ser direcionada à DLQ; **não foi executado um teste de chegada à DLQ**.

O ZIP de referência da disciplina inclui `app.js` (consumidor Node.js) e `ociHttpRequest.js` (assinatura e requisições HTTP). No Java, o SDK oficial substitui a assinatura HTTP manual. A aplicação produtora foi implementada para completar o fluxo da disciplina.

## Organização do código

```text
oci-queue-java/
├── README.md
├── RELATORIO_EVIDENCIAS.md
├── config.exemplo.ps1
├── iniciar.ps1
├── pom.xml
├── evidencias/
│   ├── 01-fila-criada.png
│   ├── 02-compilacao-testes.png
│   ├── 03-produtor-sem-canal.png
│   ├── 04-consumidor-sem-canal.png
│   ├── 05-producao-consumo-canal.png
│   ├── 06-falha-processamento.png
│   └── 07-reentrega-segunda-tentativa.png
└── src/
    ├── main/java/br/edu/unoeste/oci/
    │   ├── Main.java                       # Menu e consumidor contínuo
    │   ├── config/AppSettings.java        # Validação de variáveis externas
    │   ├── producer/MessageProducer.java  # Publicação (PutMessages)
    │   ├── consumer/MessageConsumer.java  # Consumo, delete e reentrega
    │   ├── model/EmailPayload.java        # Modelo e validação do JSON
    │   └── email/
    │       ├── EmailSender.java           # Interface de envio
    │       ├── LoggingEmailSender.java    # Processamento demonstrativo
    │       └── SmtpEmailSender.java       # Envio opcional via SMTP
    └── test/java/br/edu/unoeste/oci/model/
        └── EmailPayloadTest.java          # 3 testes JUnit
```

**Boas práticas:** responsabilidades separadas, credenciais fora do código, comentários em pontos críticos, validação do payload, tratamento de exceções, confirmação do processamento antes de excluir e testes automatizados.

## Pré-requisitos

- Java JDK 17 ou superior (testado pelo aluno com **Java 21.0.2**).
- Apache Maven (testado pelo aluno com **Maven 3.10.0**).
- VS Code ou IDE Java.
- Fila OCI ativa e usuário com permissões para enviar/consumir mensagens.
- Configuração de chave de API do usuário em `%USERPROFILE%\.oci\config`, por exemplo:

```ini
[DEFAULT]
user=ocid1.user.oc1..SEU_OCID
fingerprint=SUA_FINGERPRINT
tenancy=ocid1.tenancy.oc1..SEU_OCID
region=us-phoenix-1
key_file=C:/Users/SEU_USUARIO/.oci/oci_api_key.pem
```

**Não inclua a chave `.pem`, senhas SMTP ou seu arquivo `config` real no projeto ou nos prints.** A chave SSH de instância não substitui uma chave de assinatura da API.

## Criar e configurar a fila na OCI

No console: **Serviços ao Desenvolvedor > Filas > Criar fila**. Configuração usada no laboratório:

| Configuração | Valor |
|---|---|
| Nome | `fila-trabalho-java` |
| Compartimento | Raiz da tenancy (com permissão) |
| Timeout de visibilidade | 30 segundos |
| Retenção máxima | 1 dia |
| Máximo de tentativas de entrega | 5 |
| Consumo máximo do canal | 100% |
| Criptografia | Chave gerenciada pela Oracle |
| Grupos de consumidores premium | Desativados |

Após a fila ficar **Ativa**, copie o **OCID principal** (sem sufixo `-dlq`) e o **Ponto final de mensagens**, ambos da aba **Detalhes**.

## Compilar e executar

Abra a pasta no VS Code. No PowerShell, dentro de `oci-queue-java`:

```powershell
java -version
mvn -version
mvn clean test

$env:OCI_QUEUE_ID = "COLE_AQUI_O_OCID_DA_FILA_PRINCIPAL"
$env:OCI_QUEUE_MESSAGES_ENDPOINT = "https://cell-1.queue.messaging.us-phoenix-1.oci.oraclecloud.com"
$env:MAIL_MODE = "LOG"

.\iniciar.ps1
```

O script `iniciar.ps1` coloca a sessão do console no modo UTF-8 para reduzir problemas de acentuação no Windows e invoca `mvn compile exec:java`. A primeira execução do Maven pode baixar dependências. Se seu PowerShell bloquear scripts locais, execute manualmente os ajustes abaixo, seguidos do comando Maven:

```powershell
chcp 65001
[Console]::InputEncoding = [Console]::OutputEncoding = [System.Text.UTF8Encoding]::new($false)
$OutputEncoding = [Console]::OutputEncoding
mvn compile exec:java
```

> Na versão revisada do `pom.xml`, foi acrescentada uma implementação de logging `slf4j-simple` compatível com o aviso `StaticLoggerBinder` observado nos prints originais. **A versão revisada ainda precisa de nova compilação (`mvn clean test`) no computador do aluno**, caso seja adotada. Os prints já enviados comprovam a compilação da versão imediatamente anterior, cujo comportamento de fila foi validado.

### Menu da aplicação

| Opção | Função |
|---|---|
| `1` | Publicar sem canal |
| `2` | Publicar com canal |
| `3` | Consumir uma vez, sem filtro |
| `4` | Consumir uma vez, filtrando pelo canal |
| `5` | Consumir continuamente, sem filtro |
| `6` | Consumir continuamente, por canal |
| `0` | Encerrar |

O consumidor contínuo permanece consultando a fila até ser interrompido (por exemplo, `Ctrl+C`). Ele não foi instalado como serviço de sistema operacional, pois o enunciado fornecido não o exigiu expressamente.

## Roteiro de validação realizado

1. Executado `mvn clean test`: **3 testes executados, 0 falhas, 0 erros, BUILD SUCCESS**.
2. Opção `1`: publicada uma solicitação `teste@exemplo.com`, retornando ID de mensagem da OCI.
3. Opção `3`: recebido o **mesmo ID**, processado no modo `LOG` e excluído via OCI.
4. Opção `2`: publicada mensagem no canal `emails`.
5. Opção `4`: consumida a mesma mensagem do canal `emails`, com exclusão após sucesso.
6. Criada uma mensagem inválida diretamente na OCI (`mensagem-invalida`) e consumida: erro JSON tratado, sem exclusão e com visibilidade atualizada para 30 segundos.
7. Consultada novamente a fila: **mesmo ID com contagem de tentativa 2**, comprovando a reentrega.

Os resultados estão documentados visualmente na pasta `evidencias/`. A fila OCI utiliza entrega pelo menos uma vez: o processamento real de e-mails exigiria medidas adicionais de idempotência para evitar envios duplicados caso ocorra falha após o SMTP aceitar um envio e antes de a mensagem ser excluída.

## Envio real de e-mails (opcional e não testado)

O módulo `SmtpEmailSender` implementa envio real via SMTP com STARTTLS e credenciais em variáveis de ambiente (`SMTP_HOST`, `SMTP_PORT`, `SMTP_USERNAME`, `SMTP_PASSWORD`, `SMTP_FROM`). Para habilitá-lo, defina `MAIL_MODE=SMTP`. Não configure ou exponha credenciais reais apenas para comprovar o funcionamento da OCI Queue. Um retorno de sucesso do SMTP não garante que o e-mail tenha chegado à caixa do destinatário.

## Critérios de avaliação e limitações

- **Organização e clareza:** divisão por responsabilidade e estrutura convencional Maven.
- **Comentários explicativos:** JavaDoc e comentários nos fluxos de assinatura pelo SDK, tratamento e confirmação.
- **Boas práticas:** validação de entradas, configuração externa, reprocessamento seguro em falhas, testes JUnit.
- **Lógica central:** produtor/consumidor, payload JSON, canal, operações de publicar, receber, atualizar e excluir na OCI.
- **Não demonstrado:** envio real de e-mail SMTP; recebimento final na DLQ; execução como serviço do Windows; teste do modo contínuo. Essas funcionalidades não devem ser apresentadas como evidenciadas.
- **Caracteres incorretos nos prints anteriores:** interferência de configuração de codificação no terminal Windows; não impede a comprovação das operações da fila. Use `iniciar.ps1` para novas execuções.

## Referências

- [Visão geral das filas OCI](https://docs.oracle.com/pt-br/iaas/Content/queue/overview.htm)
- [API de filas OCI](https://docs.oracle.com/en-us/iaas/api/#/en/queue/20210201/)
- [Exemplos Java SDK](https://docs.oracle.com/en-us/iaas/tools/java-sdk-examples/latest/queue/PutMessagesExample.java.html)
- [Orientação oficial do SLF4J](https://www.slf4j.org/codes.html)
