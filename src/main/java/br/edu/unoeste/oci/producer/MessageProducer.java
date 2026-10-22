package br.edu.unoeste.oci.producer;

import br.edu.unoeste.oci.model.EmailPayload;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oracle.bmc.queue.QueueClient;
import com.oracle.bmc.queue.model.MessageMetadata;
import com.oracle.bmc.queue.model.PutMessagesDetails;
import com.oracle.bmc.queue.model.PutMessagesDetailsEntry;
import com.oracle.bmc.queue.requests.PutMessagesRequest;
import com.oracle.bmc.queue.responses.PutMessagesResponse;

import java.util.List;

/** Publica solicitações de e-mail como mensagens JSON na fila OCI. */
public final class MessageProducer {
    private final QueueClient client;
    private final String queueId;
    private final ObjectMapper json;

    public MessageProducer(QueueClient client, String queueId, ObjectMapper json) {
        this.client = client;
        this.queueId = queueId;
        this.json = json;
    }

    public void publish(String recipient, String body, String channelId) throws Exception {
        // Fazemos a serialização do payload explicitamente para preservar o esquema {email,msg}.
        String content = json.writeValueAsString(new EmailPayload(recipient, body));
        var entry = PutMessagesDetailsEntry.builder().content(content);
        if (channelId != null && !channelId.isBlank()) {
            entry.metadata(MessageMetadata.builder().channelId(channelId.trim()).build());
        }
        PutMessagesRequest request = PutMessagesRequest.builder()
                .queueId(queueId)
                .putMessagesDetails(PutMessagesDetails.builder()
                        .messages(List.of(entry.build()))
                        .build())
                .build();

        PutMessagesResponse response = client.putMessages(request);
        if (response.getPutMessages() == null || response.getPutMessages().getMessages() == null
                || response.getPutMessages().getMessages().isEmpty()) {
            throw new IllegalStateException("A OCI não confirmou a publicação da mensagem");
        }
        var result = response.getPutMessages().getMessages().get(0);
        System.out.printf("[PRODUTOR] Mensagem enviada! ID=%s, canal=%s, expiração=%s%n",
                result.getId(), (channelId == null || channelId.isBlank()) ? "padrão" : channelId,
                result.getExpireAfter());
    }
}
