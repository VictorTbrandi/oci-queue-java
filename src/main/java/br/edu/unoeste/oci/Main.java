package br.edu.unoeste.oci;

import br.edu.unoeste.oci.config.AppSettings;
import br.edu.unoeste.oci.consumer.MessageConsumer;
import br.edu.unoeste.oci.email.EmailSender;
import br.edu.unoeste.oci.email.LoggingEmailSender;
import br.edu.unoeste.oci.email.SmtpEmailSender;
import br.edu.unoeste.oci.producer.MessageProducer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oracle.bmc.ConfigFileReader;
import com.oracle.bmc.auth.ConfigFileAuthenticationDetailsProvider;
import com.oracle.bmc.queue.QueueClient;

import java.nio.charset.StandardCharsets;
import java.util.Scanner;

/** Menu de laboratório; o processo Java também pode funcionar como consumidor contínuo. */
public final class Main {
    private Main() { }

    public static void main(String[] args) {
        try {
            AppSettings settings = AppSettings.fromEnvironment();
            var credentials = new ConfigFileAuthenticationDetailsProvider(ConfigFileReader.parseDefault());
            try (QueueClient client = QueueClient.builder().build(credentials);
                 Scanner console = new Scanner(System.in, StandardCharsets.UTF_8)) {
                // Usar o endpoint de MENSAGENS da fila, não o endpoint de administração.
                client.setEndpoint(settings.messagesEndpoint());
                ObjectMapper json = new ObjectMapper();
                MessageProducer producer = new MessageProducer(client, settings.queueId(), json);
                EmailSender sender = settings.mailMode().equals("SMTP")
                        ? new SmtpEmailSender() : new LoggingEmailSender();
                MessageConsumer consumer = new MessageConsumer(client, settings.queueId(), json, sender);
                System.out.println("OCI Queue Java | Modo de e-mail: " + settings.mailMode());
                runMenu(console, producer, consumer);
            }
        } catch (Exception error) {
            System.err.println("Erro ao iniciar: " + error.getMessage());
            System.err.println("Verifique as variáveis OCI_QUEUE_ID, OCI_QUEUE_MESSAGES_ENDPOINT e ~/.oci/config.");
            System.exit(1);
        }
    }

    private static void runMenu(Scanner console, MessageProducer producer,
                                MessageConsumer consumer) {
        while (true) {
            System.out.println("\n=== OCI QUEUE — JAVA ===");
            System.out.println("1 - Produzir mensagem sem canal");
            System.out.println("2 - Produzir mensagem com canal");
            System.out.println("3 - Consumir uma vez, sem filtro");
            System.out.println("4 - Consumir uma vez, por canal");
            System.out.println("5 - Consumir continuamente, sem filtro");
            System.out.println("6 - Consumir continuamente, por canal");
            System.out.println("0 - Sair");
            System.out.print("Escolha: ");
            String option = console.nextLine().trim();
            try {
                switch (option) {
                    case "1", "2" -> {
                        System.out.print("E-mail de destino: ");
                        String email = console.nextLine().trim();
                        System.out.print("Corpo da mensagem (uma linha; HTML permitido): ");
                        String message = console.nextLine();
                        producer.publish(email, message,
                                option.equals("2") ? readChannel(console) : null);
                    }
                    case "3", "4" -> consumer.consumeOnce(
                            option.equals("4") ? readChannel(console) : null, 2);
                    case "5", "6" -> {
                        String channel = option.equals("6") ? readChannel(console) : null;
                        System.out.println("Consumidor ativo. Pressione Ctrl+C para parar.");
                        while (!Thread.currentThread().isInterrupted()) {
                            try {
                                consumer.consumeOnce(channel, 10);
                            } catch (Exception error) {
                                System.err.println("Erro temporário ao consultar OCI: " + error.getMessage());
                                Thread.sleep(5000);
                            }
                        }
                        return;
                    }
                    case "0" -> { return; }
                    default -> System.out.println("Opção inválida.");
                }
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                return;
            } catch (Exception error) {
                System.err.println("Operação não concluída: " + error.getMessage());
            }
        }
    }

    private static String readChannel(Scanner console) {
        System.out.print("ID do canal (por exemplo, emails): ");
        String channel = console.nextLine().trim();
        if (channel.isBlank()) throw new IllegalArgumentException("Informe o ID do canal.");
        return channel;
    }
}
