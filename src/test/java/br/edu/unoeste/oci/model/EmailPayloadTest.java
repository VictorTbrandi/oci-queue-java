package br.edu.unoeste.oci.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EmailPayloadTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void deveSerializarEDesserializarMensagemDeEmail() throws Exception {
        EmailPayload original = new EmailPayload("teste@exemplo.com", "Mensagem do laboratório");
        EmailPayload recuperado = mapper.readValue(mapper.writeValueAsString(original), EmailPayload.class);
        assertEquals(original, recuperado);
    }

    @Test
    void deveRejeitarDestinatarioInvalido() {
        assertThrows(IllegalArgumentException.class, () -> new EmailPayload("invalido", "teste"));
    }

    @Test
    void deveRejeitarCorpoVazio() {
        assertThrows(IllegalArgumentException.class, () -> new EmailPayload("teste@exemplo.com", ""));
    }
}
