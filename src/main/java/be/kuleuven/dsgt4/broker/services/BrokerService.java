package be.kuleuven.dsgt4.broker.services;

import com.google.api.core.ApiFuture;
import com.google.api.core.ApiFutureCallback;
import com.google.api.core.ApiFutures;
import com.google.api.gax.core.CredentialsProvider;
import com.google.api.gax.core.ExecutorProvider;
import com.google.api.gax.core.InstantiatingExecutorProvider;
import com.google.api.gax.core.NoCredentialsProvider;
import com.google.api.gax.grpc.GrpcTransportChannel;
import com.google.api.gax.rpc.ApiException;
import com.google.api.gax.rpc.FixedTransportChannelProvider;
import com.google.api.gax.rpc.StatusCode;
import com.google.api.gax.rpc.TransportChannelProvider;
import com.google.cloud.pubsub.v1.Publisher;
import com.google.cloud.pubsub.v1.SubscriptionAdminClient;
import com.google.cloud.pubsub.v1.TopicAdminClient;
import com.google.cloud.pubsub.v1.TopicAdminSettings;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.PubsubMessage;
import com.google.pubsub.v1.PushConfig;
import com.google.pubsub.v1.Subscription;
import com.google.pubsub.v1.SubscriptionName;
import com.google.pubsub.v1.TopicName;
import com.google.pubsub.v1.Topic;
import java.io.IOException;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class BrokerService {

    private static final String PROJECT_ID = "broker-da44b";

    @Autowired
    public BrokerService() {}

    public String publishMessage(String topicId, String message) throws IOException, ExecutionException, InterruptedException {
        TopicName topicName = TopicName.of(PROJECT_ID, topicId);
        System.out.println(topicName);
        Publisher publisher = null;
        try {
            publisher = Publisher.newBuilder(topicName).build();
            ByteString data = ByteString.copyFromUtf8(message);
            PubsubMessage pubsubMessage = PubsubMessage.newBuilder().setData(data).build();
            System.out.println("pubsubMessage: " + pubsubMessage);

            ApiFuture<String> messageIdFuture = publisher.publish(pubsubMessage);

            ApiFutures.addCallback(
                messageIdFuture,
                new ApiFutureCallback<String>() {
                    @Override
                    public void onFailure(Throwable throwable) {
                        if (throwable instanceof ApiException) {
                            ApiException apiException = ((ApiException) throwable);
                            System.err.println("API Exception: " + apiException.getStatusCode().getCode());
                            System.err.println("Retryable: " + apiException.isRetryable());
                        } else {
                            System.err.println("Error publishing message: " + throwable.getMessage());
                        }
                    }

                    @Override
                    public void onSuccess(String messageId) {
                        System.out.println("Published message ID: " + messageId);
                    }
                },
                MoreExecutors.directExecutor()
            );
            return messageIdFuture.get();
        } finally {
            if (publisher != null) {
                publisher.shutdown();
                publisher.awaitTermination(1, TimeUnit.MINUTES);
            }
        }
    }

    //official example code: https://cloud.google.com/docs/authentication/provide-credentials-adc#local-dev
    public String emulatorPublishMessage(String projectId, String topicId, String message){
        String hostport = System.getenv("PUBSUB_EMULATOR_HOST");
        hostport = "localhost:8100";
        System.out.println("hostport:" + hostport);
        ManagedChannel channel = ManagedChannelBuilder.forTarget(hostport).usePlaintext().build();
        try {
            TransportChannelProvider channelProvider =
                    FixedTransportChannelProvider.create(GrpcTransportChannel.create(channel));
            CredentialsProvider credentialsProvider = NoCredentialsProvider.create();
            // Set the channel and credentials provider when creating a `TopicAdminClient`.
            // Similarly for SubscriptionAdminClient
            TopicAdminClient topicClient =
                    TopicAdminClient.create(
                            TopicAdminSettings.newBuilder()
                                    .setTransportChannelProvider(channelProvider)
                                    .setCredentialsProvider(credentialsProvider)
                                    .build());
            TopicName topicName = TopicName.of(projectId, topicId);
            // Set the channel and credentials provider when creating a `Publisher`.
            // Similarly for Subscriber
            Publisher publisher =
                    Publisher.newBuilder(topicName)
                            .setChannelProvider(channelProvider)
                            .setCredentialsProvider(credentialsProvider)
                            .build();
            ByteString data = ByteString.copyFromUtf8(message);
            PubsubMessage pubsubMessage = PubsubMessage.newBuilder().setData(data).build();
            ApiFuture<String> messageIdFuture = publisher.publish(pubsubMessage);
            String messageId = messageIdFuture.get();
            System.out.println("Emulator test: projectId " + projectId + " topicId " + topicId + "messageId " + messageId);
            return messageId;
        } catch (IOException | InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        } finally {
            channel.shutdown();
        }
    }

    public String publishMessage(String topicId, Map<String, Object> message) throws IOException, ExecutionException, InterruptedException {
        TopicName topicName = TopicName.of(PROJECT_ID, topicId);
        Publisher publisher = null;
        AtomicReference<String> clientMessage = new AtomicReference<>("");
        try {
            // Provides an executor service for processing messages. The default
            // `executorProvider` used by the publisher has a default thread count of
            // 5 * the number of processors available to the Java virtual machine.
            ExecutorProvider executorProvider = InstantiatingExecutorProvider.newBuilder().setExecutorThreadCount(10).build();

            publisher = Publisher.newBuilder(topicName).setEnableMessageOrdering(true).setExecutorProvider(executorProvider).build();

            // Convert the map to attributes
            Map<String, String> attributes = new HashMap<>();
            for (Map.Entry<String, Object> entry : message.entrySet()) {
                attributes.put(entry.getKey(), entry.getValue().toString());
            }

            PubsubMessage pubsubMessage = PubsubMessage.newBuilder()
                    .putAllAttributes(attributes)
                    .build();

            ApiFuture<String> messageIdFuture = publisher.publish(pubsubMessage);
            AtomicBoolean isRetryable = new AtomicBoolean();
            ApiFutures.addCallback(
                messageIdFuture,
                new ApiFutureCallback<String>() {
                    @Override
                    public void onFailure(Throwable throwable) {
                        if (throwable instanceof ApiException) {
                            ApiException apiException = ((ApiException) throwable);
                            System.err.println("API Exception: " + apiException.getStatusCode().getCode());
                            System.err.println("Retryable: " + apiException.isRetryable());
                            isRetryable.set(apiException.isRetryable());
                            clientMessage.set(shouldRetry(apiException.getStatusCode().getCode()));
                        } else {
                            System.err.println("Error publishing message: " + throwable.getMessage());
                        }
                    }

                    @Override
                    public void onSuccess(String messageId) {
                        System.out.println("Published message ID: " + messageId);
                        clientMessage.set("SUCCESS");
                    }
                },
                MoreExecutors.directExecutor()
            );
            // print into console and system out the message id future and return it
            System.out.println("Message ID Future: " + messageIdFuture);
            System.out.println("Message ID Future get: " + messageIdFuture.get());

//            return isRetryable.get() ? messageIdFuture.get() : null;
            return clientMessage.get();
        } finally {
            if (publisher != null) {
                publisher.shutdown();
                publisher.awaitTermination(1, TimeUnit.MINUTES);
            }
        }
    }

    private String shouldRetry(StatusCode.Code statusCode) {
        switch (statusCode) {
            case ABORTED:
                return "ABORTED";
            case CANCELLED:
                return "CANCELLED";
            case DEADLINE_EXCEEDED:
                return "DEADLINE_EXCEEDED";
            case INTERNAL:
                return "INTERNAL";
            case RESOURCE_EXHAUSTED:
                return "RESOURCE_EXHAUSTED";
            case UNAVAILABLE:
                return "UNAVAILABLE";
            case UNKNOWN:
                return "UNKNOWN";
            default:
                return "NO_RETRY";
        }
    }

    public static void createTopic(String projectId, String topicId) throws IOException {
        try (TopicAdminClient topicAdminClient = TopicAdminClient.create()) {
          TopicName topicName = TopicName.of(projectId, topicId);
          Topic topic = topicAdminClient.createTopic(topicName);
          System.out.println("Created topic: " + topic.getName());
        }
    }

    public static void createPushSubscription(String projectId, String subscriptionId, String topicId, String pushEndpoint) throws IOException {
        try (SubscriptionAdminClient subscriptionAdminClient = SubscriptionAdminClient.create()) {
            TopicName topicName = TopicName.of(projectId, topicId);
            SubscriptionName subscriptionName = SubscriptionName.of(projectId, subscriptionId);
            PushConfig pushConfig = PushConfig.newBuilder()
                                                .setPushEndpoint(pushEndpoint)
                                                .build();
            Subscription subscription = subscriptionAdminClient.createSubscription(subscriptionName, topicName, pushConfig, 10);
            System.out.println("Created push subscription: " + subscription.getName());
        }
    }
}
