package be.kuleuven.dsgt4.flightRestService.services;

import com.google.cloud.pubsub.v1.AckReplyConsumer;
import com.google.cloud.pubsub.v1.MessageReceiver;
import com.google.cloud.pubsub.v1.Subscriber;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.pubsub.v1.ProjectSubscriptionName;
import com.google.pubsub.v1.PubsubMessage;

import be.kuleuven.dsgt4.flightRestService.domain.FlightEvent;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.ResponseEntity;

import com.google.api.gax.rpc.ApiException;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class FlightService {
    private static final String PROJECT_ID = "broker-da44b";
    private static final String SUBSCRIPTION_ID = "your-subscription-id";

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    public void startSubscriber() throws Exception {
        ProjectSubscriptionName subscriptionName = ProjectSubscriptionName.of(PROJECT_ID, SUBSCRIPTION_ID);

        MessageReceiver receiver =
                (PubsubMessage message, AckReplyConsumer consumer) -> {
                // Parse the message data into a Map
                Map<String, Object> messageData = new Gson().fromJson(message.getData().toStringUtf8(), new TypeToken<Map<String, Object>>(){}.getType());

                // Publish an event
                eventPublisher.publishEvent(new FlightEvent(this, messageData));

                // Acknowledge the message
                consumer.ack();
                };

        Subscriber subscriber = null;
        try {
            subscriber = Subscriber.newBuilder(subscriptionName, receiver).build();
            subscriber.addListener(
                new Subscriber.Listener() {
                    public void failed(Subscriber.State from, Throwable failure) {
                        if (failure instanceof ApiException) {
                            ApiException apiException = ((ApiException) failure);
                            // handle ApiException here
                            System.out.println("Error: " + apiException.getStatusCode().getCode());
                            System.out.println("Is retryable: " + apiException.isRetryable());
                        } else {
                            // handle other exceptions
                            System.out.println("Error: " + failure.getMessage());
                        }
                    }
                },
                MoreExecutors.directExecutor()
            );
            subscriber.startAsync().awaitRunning();
            // Continue to listen to messages
            while (true) {
                // do something else
            }
        } finally {
            if (subscriber != null) {
                subscriber.stopAsync();
            }
        }
    }
}