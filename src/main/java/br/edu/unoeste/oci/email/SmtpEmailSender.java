package br.edu.unoeste.oci.email;

import br.edu.unoeste.oci.config.AppSettings;
import br.edu.unoeste.oci.model.EmailPayload;
import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.util.Properties;

/** Envia e-mails via SMTP; credenciais vêm exclusivamente de variáveis de ambiente. */
public final class SmtpEmailSender implements EmailSender {
    private final Session session;
    private final String from;

    public SmtpEmailSender() {
        String host = AppSettings.required("SMTP_HOST");
        String port = System.getenv().getOrDefault("SMTP_PORT", "587");
        String user = AppSettings.required("SMTP_USERNAME");
        String password = AppSettings.required("SMTP_PASSWORD");
        from = AppSettings.required("SMTP_FROM");

        Properties properties = new Properties();
        properties.put("mail.smtp.host", host);
        properties.put("mail.smtp.port", port);
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.starttls.enable", "true");
        properties.put("mail.smtp.starttls.required", "true");
        properties.put("mail.smtp.connectiontimeout", "10000");
        properties.put("mail.smtp.timeout", "10000");
        properties.put("mail.smtp.writetimeout", "10000");
        session = Session.getInstance(properties, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(user, password);
            }
        });
    }

    @Override
    public void send(EmailPayload payload) throws Exception {
        MimeMessage email = new MimeMessage(session);
        email.setFrom(new InternetAddress(from));
        email.setRecipients(Message.RecipientType.TO, InternetAddress.parse(payload.email(), true));
        email.setSubject("Teste Fila OCI", "UTF-8");
        email.setContent(payload.msg(), "text/html; charset=UTF-8");
        // Importante: aguarda confirmação (ou erro) do servidor SMTP antes de excluir da fila.
        Transport.send(email);
        System.out.println("[SMTP] Provedor SMTP aceitou o envio para " + payload.email());
    }
}
