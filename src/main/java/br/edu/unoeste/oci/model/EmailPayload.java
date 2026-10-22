package br.edu.unoeste.oci.model;

/** Corpo JSON enviado pela aplicação produtora e interpretado pela consumidora. */
public record EmailPayload(String email, String msg) {
    public EmailPayload {
        if (email == null || email.isBlank() || !email.contains("@")) {
            throw new IllegalArgumentException("Destinatário de e-mail inválido");
        }
        if (msg == null || msg.isBlank()) {
            throw new IllegalArgumentException("O texto da mensagem não pode estar vazio");
        }
    }
}
