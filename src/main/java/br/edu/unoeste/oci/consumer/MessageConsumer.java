package br.edu.unoeste.oci.consumer;

import br.edu.unoeste.oci.email.EmailSender;
import br.edu.unoeste.oci.model.EmailPayload;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oracle.bmc.queue.QueueClient;
import com.oracle.bmc.queue.model.GetMessage;
import com.oracle.bmc.queue.model.UpdateMessageDetails;
import com.oracle.bmc.queue.requests.DeleteMessageRequest;
import com.oracle.bmc.queue.requests.GetMessagesRequest;
import com.oracle.bmc.queue.requests.UpdateMessageRequest;

import java.util.List;

/** Consumidor OCI: recebe, processa e confirma mensagens; falhas permitem reentrega. */
public final class MessageConsumer {
    private final QueueClient client;
    private final String queueId;
    private final ObjectMapper json;
    private final EmailSender emailSender;

    public MessageConsumer(QueueClient client, String queueId, ObjectMapper json, EmailSender emailSender) {
        this.client = client;
        this.queueId = queueId;
        this.json = json;
        this.emailSender = emailSender;
    }

    /** Executa uma consulta e processa até dez mensagens. */
    public int consumeOnce(String channelId, int pollingSeconds) {
        var builder = GetMessagesRequest.builder()
                .queueId(queueId)
                .limit(10)
                .timeoutInSeconds(pollingSeconds);
        if (channelId != null && !channelId.isBlank()) {
            builder.channelFilter(channelId.trim());
        }

        List<GetMessage> messages = client.getMessages(builder.build()).getGetMessages().getMessages();
        if (messages == null || messages.isEmpty()) {
            System.out.println("[CONSUMIDOR] Nenhuma mensagem disponível nesta consulta.");
            return 0;
        }
        System.out.println("[CONSUMIDOR] Mensagens recebidas: " + messages.size());
        for (GetMessage message : messages) {
            process(message);
        }
        return messages.size();
    }

    private void process(GetMessage message) {
        String receipt = message.getReceipt();
        System.out.printf("[CONSUMIDOR] ID=%s, tentativa=%s, conteúdo=%s%n",
                message.getId(), message.getDeliveryCount(), message.getContent());
        try {
            // O exemplo Node.js lê email/msg diretamente da mensagem. Em OCI estão no content JSON.
            EmailPayload payload = json.readValue(message.getContent(), EmailPayload.class);
            // Se o SMTP falhar, a exceção impede a exclusão da mensagem.
            emailSender.send(payload);
            client.deleteMessage(DeleteMessageRequest.builder()
                    .queueId(queueId)
                    .messageReceipt(receipt)
                    .build());
            System.out.println("[CONSUMIDOR] Mensagem processada e excluída da OCI.");
        } catch (Exception error) {
            System.err.println("[CONSUMIDOR] Falha no processamento: " + error.getMessage());
            // Reprograma visibilidade em 30s para permitir nova tentativa / DLQ.
            // A exclusão NÃO é efetuada se o processamento falhou.
            try {
                client.updateMessage(UpdateMessageRequest.builder()
                        .queueId(queueId)
                        .messageReceipt(receipt)
                        .updateMessageDetails(UpdateMessageDetails.builder()
                                .visibilityInSeconds(30)
                                .build())
                        .build());
                System.out.println("[CONSUMIDOR] Nova tentativa poderá ocorrer após 30 segundos.");
            } catch (Exception updateError) {
                System.err.println("[CONSUMIDOR] Falha ao atualizar visibilidade: " + updateError.getMessage());
            }
        }
    }
}
