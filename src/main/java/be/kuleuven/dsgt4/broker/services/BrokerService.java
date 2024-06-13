package be.kuleuven.dsgt4.broker.services;

import com.google.api.core.ApiFuture;
import com.google.cloud.pubsub.v1.Publisher;
import com.google.cloud.pubsub.v1.SubscriptionAdminClient;
import com.google.gson.Gson;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Service
public class BrokerService {

    private static final String PROJECT_ID = "broker-da44b";
    private final Gson gson = new Gson();

    @Autowired
    public BrokerService() {}

    public String publishMessage(String topicId, String message) throws IOException, ExecutionException, InterruptedException {
        TopicName topicName = TopicName.of(PROJECT_ID, topicId);
        Publisher publisher = null;
        try {
            publisher = Publisher.newBuilder(topicName).build();
            ByteString data = ByteString.copyFromUtf8(message);
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

    public String publishMessage(String topicId, Map<String, Object> message) throws IOException, ExecutionException, InterruptedException {
        String jsonMessage = gson.toJson(message);
        return publishMessage(topicId, jsonMessage);
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


/**
package be.kuleuven.dsgt4.broker.services;

import com.google.api.core.ApiFuture;
import com.google.cloud.pubsub.v1.Publisher;
import com.google.cloud.pubsub.v1.SubscriptionAdminClient;
import com.google.gson.Gson;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import be.kuleuven.dsgt4.broker.domain.TravelPackage;
import be.kuleuven.dsgt4.broker.domain.ItemType;

@Service
public class BrokerPublisherService {

    // Environment variables
    private static final String PROJECT_ID = "broker-da44b";
    private static final String PUSH_ENDPOINT = "https://my-test-project.appspot.com/push";
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final List<TravelPackage> travelPackages = new ArrayList<>();
    private final TransactionCoordinatorService transactionCoordinatorService;
    private final Gson gson = new Gson();

    @Autowired
    public BrokerPublisherService(TransactionCoordinatorService transactionCoordinatorService) {
        this.transactionCoordinatorService = transactionCoordinatorService;
    }

    // Publish a message to a specific topic
    public String publishMessage(String topicId, String message) throws IOException, ExecutionException, InterruptedException {
        TopicName topicName = TopicName.of(PROJECT_ID, topicId);
        Publisher publisher = null;
        try {
            publisher = Publisher.newBuilder(topicName).build();
            ByteString data = ByteString.copyFromUtf8(message);
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

    public String publishMessage(String topicId, Map<String, Object> message) throws IOException, ExecutionException, InterruptedException {
        String jsonMessage = gson.toJson(message);
        return publishMessage(topicId, jsonMessage);
    }

    public static void createPushSubscriptionExample(String projectId, String subscriptionId, String topicId, String pushEndpoint) throws IOException {
        try (SubscriptionAdminClient subscriptionAdminClient = SubscriptionAdminClient.create()) {
            TopicName topicName = TopicName.of(projectId, topicId);
            SubscriptionName subscriptionName = SubscriptionName.of(projectId, subscriptionId);
            PushConfig pushConfig = PushConfig.newBuilder().setPushEndpoint(pushEndpoint).build();
            Subscription subscription = subscriptionAdminClient.createSubscription(subscriptionName, topicName, pushConfig, 10);
            System.out.println("Created push subscription: " + subscription.getName());
        }
    }

    public void handleBookingResponse(String message) {
        transactionCoordinatorService.processBookingResponse(message);
    }

    // Methods related to TravelPackage management
    public TravelPackage createTravelPackage(String userId) {
        String packageId = generatePackageId();
        TravelPackage travelPackage = new TravelPackage(userId, packageId);
        travelPackages.add(travelPackage);
        return travelPackage;
    }

    public TravelPackage getTravelPackage(String userId, String packageId) {
        return travelPackages.stream()
                .filter(pkg -> pkg.getUserId().equals(userId) && pkg.getPackageId().equals(packageId))
                .findFirst()
                .orElse(null);
    }

    public boolean clearTravelPackage(String userId, String packageId) {
        TravelPackage travelPackage = getTravelPackage(userId, packageId);
        if (travelPackage != null) {
            travelPackage.clear();
            return true;
        }
        return false;
    }

    public boolean cancelTravelPackage(String userId, String packageId) {
        TravelPackage travelPackage = getTravelPackage(userId, packageId);
        if (travelPackage != null) {
            travelPackage.getFlights().forEach(flightJson -> {
                Map<String, Object> flightMap = parseJson(flightJson);
                String flightId = (String) flightMap.get("flightId");
                int availableSeats = (int) flightMap.get("seats");
                cancelFlight(flightId, availableSeats);
            });

            travelPackage.getHotels().forEach(hotelJson -> {
                Map<String, Object> hotelMap = parseJson(hotelJson);
                String hotelId = (String) hotelMap.get("hotelId");
                int availableRooms = (int) hotelMap.get("rooms");
                cancelHotel(hotelId, availableRooms);
            });

            travelPackage.clear();
            return true;
        }
        return false;
    }

    public boolean cancelItemInTravelPackage(String userId, String packageId, ItemType type, String itemId) {
        TravelPackage travelPackage = getTravelPackage(userId, packageId);
        if (travelPackage == null) {
            return false;
        }

        if (type == ItemType.FLIGHT) {
            return cancelFlightInPackage(travelPackage, itemId);
        } else if (type == ItemType.HOTEL) {
            return cancelHotelInPackage(travelPackage, itemId);
        }
        return false;
    }

    private boolean cancelFlightInPackage(TravelPackage travelPackage, String flightId) {
        for (String flightJson : travelPackage.getFlights()) {
            Map<String, Object> flightMap = parseJson(flightJson);
            String currentFlightId = (String) flightMap.get("flightId");
            if (currentFlightId.equals(flightId)) {
                int availableSeats = (int) flightMap.get("seats");
                cancelFlight(flightId, availableSeats);
                travelPackage.removeFlight(flightJson);
                return true;
            }
        }
        return false;
    }

    private boolean cancelHotelInPackage(TravelPackage travelPackage, String hotelId) {
        for (String hotelJson : travelPackage.getHotels()) {
            Map<String, Object> hotelMap = parseJson(hotelJson);
            String currentHotelId = (String) hotelMap.get("hotelId");
            if (currentHotelId.equals(hotelId)) {
                int availableRooms = (int) hotelMap.get("rooms");
                cancelHotel(hotelId, availableRooms);
                travelPackage.removeHotel(hotelJson);
                return true;
            }
        }
        return false;
    }

    public void addFlightToTravelPackage(TravelPackage travelPackage, String flightJson) {
        travelPackage.addFlight(flightJson);
    }

    public void removeFlightFromTravelPackage(TravelPackage travelPackage, String flightJson) {
        travelPackage.removeFlight(flightJson);
    }

    public void addHotelToTravelPackage(TravelPackage travelPackage, String hotelJson) {
        travelPackage.addHotel(hotelJson);
    }

    public void removeHotelFromTravelPackage(TravelPackage travelPackage, String hotelJson) {
        travelPackage.removeHotel(hotelJson);
    }

    public String getHotels() throws IOException, ExecutionException, InterruptedException {
        return publishMessage("hotel-retrieve-requests", "{}");
    }

    public String getHotel(String id) throws IOException, ExecutionException, InterruptedException {
        return publishMessage("hotel-retrieve-requests", "{\"id\":\"" + id + "\"}");
    }

    public boolean bookHotel(String hotelId, int rooms) {
        try {
            String bookingDetails = "{\"hotelId\":\"" + hotelId + "\",\"rooms\":" + rooms + "}";
            publishMessage("hotel-booking-requests", bookingDetails);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public String getFlights() throws IOException, ExecutionException, InterruptedException {
        return publishMessage("flight-retrieve-requests", "{}");
    }

    public String getFlight(String id) throws IOException, ExecutionException, InterruptedException {
        return publishMessage("flight-retrieve-requests", "{\"id\":\"" + id + "\"}");
    }

    public boolean bookFlight(String flightId, int seats) {
        try {
            String bookingDetails = "{\"flightId\":\"" + flightId + "\",\"seats\":" + seats + "}";
            publishMessage("flight-booking-requests", bookingDetails);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean cancelFlight(String flightId, int seats) {
        try {
            String cancelDetails = "{\"flightId\":\"" + flightId + "\",\"seats\":" + seats + "}";
            publishMessage("flight-cancel-requests", cancelDetails);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean cancelHotel(String hotelId, int rooms) {
        try {
            String cancelDetails = "{\"hotelId\":\"" + hotelId + "\",\"rooms\":" + rooms + "}";
            publishMessage("hotel-cancel-requests", cancelDetails);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private Map<String, Object> parseJson(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse JSON", e);
        }
    }

    private String generatePackageId() {
        return "package-" + System.currentTimeMillis(); // Simple ID generation logic
    }
}
*/