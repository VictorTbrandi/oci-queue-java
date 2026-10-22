package br.edu.unoeste.oci.email;

import br.edu.unoeste.oci.model.EmailPayload;

/** Modo demonstrativo: processa a solicitação sem disparar e-mail de verdade. */
public final class LoggingEmailSender implements EmailSender {
    @Override
    public void send(EmailPayload payload) {
        System.out.printf("[SIMULAÇÃO] E-mail para %s: %s%n", payload.email(), payload.msg());
        System.out.println("[SIMULAÇÃO] Processamento local concluído; nenhum e-mail real foi enviado.");
    }
}
