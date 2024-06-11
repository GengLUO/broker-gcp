package be.kuleuven.dsgt4.broker.services;

import com.google.api.core.ApiFuture;
import com.google.cloud.pubsub.v1.Publisher;
import com.google.cloud.pubsub.v1.SubscriptionAdminClient;
import com.google.pubsub.v1.*;
import com.google.protobuf.ByteString;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Service
public class BrokerPublisherService {

//    TODO: these can be env variables
    private static final String PROJECT_ID = "broker-da44b";
    private static final String TOPIC_ID = "your-topic-id";
    // For now, we have:
    // 1. hotel-booking-requests
    // 2. flight-booking-requests
    private static final String PUSH_ENDPOINT = "https://your-domain.com/bookings/pubsub/push";
    private final TransactionCoordinatorService transactionCoordinatorService;

    @Autowired
    public BrokerPublisherService(TransactionCoordinatorService transactionCoordinatorService) {
        this.transactionCoordinatorService = transactionCoordinatorService;
    }

    // Official example code for publishing messages
    public String publishMessageExample(String message) throws IOException, ExecutionException, InterruptedException {
        TopicName topicName = TopicName.of(PROJECT_ID, TOPIC_ID);
        Publisher publisher = null;
        try {
            // Create a publisher instance with default settings bound to the topic
            publisher = Publisher.newBuilder(topicName).build();
            ByteString data = ByteString.copyFromUtf8(message);
            PubsubMessage pubsubMessage = PubsubMessage.newBuilder().setData(data).build();

            // Once published, returns a server-assigned message id (unique within the topic)
            ApiFuture<String> messageIdFuture = publisher.publish(pubsubMessage);
            String messageId = messageIdFuture.get();
            System.out.println("Published message ID: " + messageId);
            return messageId;
            // TODO: use this ? https://cloud.google.com/pubsub/docs/publisher
        } finally {
            if (publisher != null) {
                // When finished with the publisher, shutdown to free up resources.
                publisher.shutdown();
                publisher.awaitTermination(1, TimeUnit.MINUTES);
            }
        }
    }

    // Use a param topicId
    public String publishMessage(String topicId, String message) throws IOException, ExecutionException, InterruptedException {
        TopicName topicName = TopicName.of(PROJECT_ID, topicId);
        Publisher publisher = null;
        try {
            // Create a publisher instance with default settings bound to the topic
            publisher = Publisher.newBuilder(topicName).build();
            ByteString data = ByteString.copyFromUtf8(message);
            PubsubMessage pubsubMessage = PubsubMessage.newBuilder().setData(data).build();

            // Once published, returns a server-assigned message id (unique within the topic)
            ApiFuture<String> messageIdFuture = publisher.publish(pubsubMessage);
            String messageId = messageIdFuture.get();
            System.out.println("Published message ID: " + messageId);
            return messageId;
        } finally {
            if (publisher != null) {
                // When finished with the publisher, shutdown to free up resources.
                publisher.shutdown();
                publisher.awaitTermination(1, TimeUnit.MINUTES);
            }
        }
    }

    //https://console.cloud.google.com/cloudpubsub/subscription/list?project=broker-da44b&supportedpurview=project
    public void createPushSubscription(String subscriptionId, String topicId) throws IOException {
        try (SubscriptionAdminClient subscriptionAdminClient = SubscriptionAdminClient.create()) {
            TopicName topicName = TopicName.of(PROJECT_ID, topicId);
            SubscriptionName subscriptionName = SubscriptionName.of(PROJECT_ID, subscriptionId);
            PushConfig pushConfig = PushConfig.newBuilder().setPushEndpoint(PUSH_ENDPOINT).build();

            // Create a push subscription with default acknowledgement deadline of 10 seconds.
            // Messages not successfully acknowledged within 10 seconds will get resent by the server.
            // TODO: adjust the ack time
            Subscription subscription =
                    subscriptionAdminClient.createSubscription(subscriptionName, topicName, pushConfig, 10);
            System.out.println("Created push subscription: " + subscription.getName());
        }
    }

    public void handleBookingResponse(String message) {
        // Process the message and coordinate the transaction
        transactionCoordinatorService.processBookingResponse(message);
    }
}
