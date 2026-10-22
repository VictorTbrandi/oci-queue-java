package br.edu.unoeste.oci.email;

import br.edu.unoeste.oci.model.EmailPayload;

/** Contrato que permite separar a lógica de consumo da estratégia de envio. */
@FunctionalInterface
public interface EmailSender {
    void send(EmailPayload payload) throws Exception;
}
