import io.nats.client.Message;
import io.nats.client.MessageHandler;
import provider.NatsProvider;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;

public class subscribe_by_seed {

    public static void main(String[] args) {
        String accessToken = "access_token";
        String natsUrl = "nats://url.com";
        String streamName = "stream";
        try {
            NatsProvider natsProvider = new NatsProvider(accessToken, natsUrl, streamName);
            MessageHandler connectionMessageHandler = new MessageHandler() {
                @Override
                public void onMessage(Message message) throws InterruptedException {
                    String connectionResponse = new String(message.getData(), StandardCharsets.UTF_8);
                    System.out.println("Connection Message: " + connectionResponse);
                }
            };

            MessageHandler subscribeMessageHandler = new MessageHandler() {
                @Override
                public void onMessage(Message message) throws InterruptedException {
                    String messageResponse = new String(message.getData(), StandardCharsets.UTF_8);
                    System.out.println("Message Receive: " + messageResponse);

                }
            };

            natsProvider.connect(connectionMessageHandler);
            natsProvider.subscribe(subscribeMessageHandler);

        } catch (GeneralSecurityException | IOException | InterruptedException e) {
            throw new RuntimeException(e);
        } catch (Exception ignored) {

        }
    }
}