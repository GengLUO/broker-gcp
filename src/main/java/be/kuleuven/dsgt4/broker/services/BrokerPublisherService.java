package be.kuleuven.dsgt4.broker.services;

import com.google.api.core.ApiFuture;
import com.google.cloud.pubsub.v1.Publisher;
import com.google.cloud.pubsub.v1.SubscriptionAdminClient;
import com.google.pubsub.v1.*;
import com.google.protobuf.ByteString;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import com.google.gson.Gson;

@Service
public class BrokerPublisherService {

    private static final String PROJECT_ID = "broker-da44b";
    private static final String TOPIC_ID = "your-topic-id";
    private static final String PUSH_ENDPOINT = "https://your-domain.com/bookings/pubsub/push";
    private final TransactionCoordinatorService transactionCoordinatorService;
    private final Gson gson = new Gson();

    @Autowired
    public BrokerPublisherService(TransactionCoordinatorService transactionCoordinatorService) {
        this.transactionCoordinatorService = transactionCoordinatorService;
    }

    public String publishMessage(String topicId, Map<String, Object> message) throws IOException, ExecutionException, InterruptedException {
        TopicName topicName = TopicName.of(PROJECT_ID, topicId);
        Publisher publisher = null;
        try {
            publisher = Publisher.newBuilder(topicName).build();
            String jsonMessage = gson.toJson(message);
            ByteString data = ByteString.copyFromUtf8(jsonMessage);
            PubsubMessage pubsubMessage = PubsubMessage.newBuilder().setData(data).build();
            ApiFuture<String> messageIdFuture = publisher.publish(pubsubMessage);
            String messageId = messageIdFuture.get();
            System.out.println("Published message ID: " + messageId);
            return messageId;
        } finally {
            if (publisher != null) {
                publisher.shutdown();
                publisher.awaitTermination(1, TimeUnit.MINUTES);
            }
        }
    }

    public static void createPushSubscriptionExample(
            String projectId, String subscriptionId, String topicId, String pushEndpoint)
            throws IOException {
        try (SubscriptionAdminClient subscriptionAdminClient = SubscriptionAdminClient.create()) {
            TopicName topicName = TopicName.of(projectId, topicId);
            SubscriptionName subscriptionName = SubscriptionName.of(projectId, subscriptionId);
            PushConfig pushConfig = PushConfig.newBuilder().setPushEndpoint(pushEndpoint).build();
            Subscription subscription =
                    subscriptionAdminClient.createSubscription(subscriptionName, topicName, pushConfig, 10);
            System.out.println("Created push subscription: " + subscription.getName());
        }
    }

    public void handleBookingResponse(String message) {
        transactionCoordinatorService.processBookingResponse(message);
    }
}
