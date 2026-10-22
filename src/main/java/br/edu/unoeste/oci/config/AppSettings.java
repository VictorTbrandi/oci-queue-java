package br.edu.unoeste.oci.config;

/** Configuração de infraestrutura externa ao código-fonte. */
public record AppSettings(String queueId, String messagesEndpoint, String mailMode) {
    public static AppSettings fromEnvironment() {
        String queueId = required("OCI_QUEUE_ID");
        String endpoint = required("OCI_QUEUE_MESSAGES_ENDPOINT");
        if (!endpoint.startsWith("https://")) {
            throw new IllegalArgumentException("O endpoint de mensagens precisa começar com https://");
        }
        if (endpoint.endsWith("/")) endpoint = endpoint.substring(0, endpoint.length() - 1);
        String mailMode = System.getenv().getOrDefault("MAIL_MODE", "LOG").trim().toUpperCase();
        if (!mailMode.equals("LOG") && !mailMode.equals("SMTP")) {
            throw new IllegalArgumentException("MAIL_MODE deve ser LOG ou SMTP");
        }
        return new AppSettings(queueId, endpoint, mailMode);
    }

    public static String required(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Variável de ambiente não configurada: " + name);
        }
        return value.trim();
    }
}
