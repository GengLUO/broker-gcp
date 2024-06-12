package be.kuleuven.dsgt4.broker.services;

import com.google.api.core.ApiFuture;
import com.google.cloud.pubsub.v1.Publisher;
import com.google.cloud.pubsub.v1.SubscriptionAdminClient;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.*;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.ObjectMapper;

import be.kuleuven.dsgt4.broker.domain.TravelPackage;
import be.kuleuven.dsgt4.broker.domain.ItemType;
import com.fasterxml.jackson.core.type.TypeReference;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


@Service
public class BrokerPublisherService {

//    TODO: these can be env variables
    private static final String PROJECT_ID = "broker-da44b";
    private static final String TOPIC_ID = "your-topic-id";
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final List<TravelPackage> travelPackages = new ArrayList<>();
    //For now, we have:
//    1. hotel-booking-requests
//    2. flight-booking-requests
    private static final String PUSH_ENDPOINT = "https://my-test-project.appspot.com/push";

    //official example code for publish messages
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
//            TODO: use this ? https://cloud.google.com/pubsub/docs/publisher
        } finally {
            if (publisher != null) {
                // When finished with the publisher, shutdown to free up resources.
                publisher.shutdown();
                publisher.awaitTermination(1, TimeUnit.MINUTES);
            }
        }
    }

    //use a param topicId
    //方法返回一个字符串，即发布消息后, 返回的消息 ID
    public String publishMessage(String topicId, String message) throws IOException, ExecutionException, InterruptedException {
        TopicName topicName = TopicName.of(PROJECT_ID, topicId);
        Publisher publisher = null;
        try {
            // Create a publisher instance with default settings bound to the topic
            publisher = Publisher.newBuilder(topicName).build();
            ByteString data = ByteString.copyFromUtf8(message); //将字符串消息转换为 ByteString 对象，这是 Google Protobuf 使用的字节串表示形式
            PubsubMessage pubsubMessage = PubsubMessage.newBuilder().setData(data).build(); //PubsubMessage 是 Google Cloud Pub/Sub 中的消息格式。

            // Once published, returns a server-assigned message id (unique within the topic)
            ApiFuture<String> messageIdFuture = publisher.publish(pubsubMessage);
            String messageId = messageIdFuture.get(); //调用 get() 方法阻塞当前线程，直到消息发布完成，并获取服务器分配的消息 ID
            System.out.println("Published message ID: " + messageId);
            return messageId;
        } finally {
            if (publisher != null) {
                // When finished with the publisher, shutdown to free up resources.
                publisher.shutdown();
                //等待 Publisher 关闭完成，最长等待 1 分钟。确保所有发布操作完成后再关闭 Publisher
                publisher.awaitTermination(1, TimeUnit.MINUTES);
            }
        }
    }

    //https://console.cloud.google.com/cloudpubsub/subscription/list?project=broker-da44b&supportedpurview=project
    //similar to above
    public static void createPushSubscriptionExample(
            String projectId, String subscriptionId, String topicId, String pushEndpoint)
            throws IOException {
        try (SubscriptionAdminClient subscriptionAdminClient = SubscriptionAdminClient.create()) {
            TopicName topicName = TopicName.of(projectId, topicId);
            SubscriptionName subscriptionName = SubscriptionName.of(projectId, subscriptionId);
            PushConfig pushConfig = PushConfig.newBuilder().setPushEndpoint(pushEndpoint).build();

            // Create a push subscription with default acknowledgement deadline of 10 seconds.
            // Messages not successfully acknowledged within 10 seconds will get resent by the server.
//            TODO: adjust the ack time
            Subscription subscription =
                    subscriptionAdminClient.createSubscription(subscriptionName, topicName, pushConfig, 10);
            System.out.println("Created push subscription: " + subscription.getName());
        }
    }


    /***************All methods below are the real Broker services***************/
    public TravelPackage createTravelPackage(String userId) {
        TravelPackage travelPackage = new TravelPackage(userId);
        travelPackages.add(travelPackage);
        return travelPackage;
    }

    public TravelPackage getTravelPackage(String userId) {
        for (TravelPackage travelPackage : travelPackages) {
            if (travelPackage.getUserId().equals(userId)) {
                return travelPackage;
            }
        }
        return null;
    }

    public boolean bookTravelPackage(String userId) {
        TravelPackage travelPackage = getTravelPackage(userId);
        if (travelPackage == null) {
            return false;
        }

        List<String> bookedFlights = new ArrayList<>();
        List<String> bookedHotels = new ArrayList<>();

        try {
            for (String flightJson : travelPackage.getFlights()) {
                Map<String, Object> flightMap = parseJson(flightJson);
                Long flightId = (Long) flightMap.get("id");
                int availableSeats = (int) flightMap.get("availableSeats");
                boolean booked = bookFlight(flightId, availableSeats);
                if (!booked) {
                    throw new Exception("Failed to book flight: " + flightId);
                }
                bookedFlights.add(flightJson);
            }

            for (String hotelJson : travelPackage.getHotels()) {
                Map<String, Object> hotelMap = parseJson(hotelJson);
                Long hotelId = (Long) hotelMap.get("id");
                int availableRooms = (int) hotelMap.get("availableRooms");
                boolean booked = bookHotel(hotelId, availableRooms);
                if (!booked) {
                    throw new Exception("Failed to book hotel: " + hotelId);
                }
                bookedHotels.add(hotelJson);
            }

            return true;
        } catch (Exception e) {
            System.err.println(e.getMessage());
            for (String flightJson : bookedFlights) {
                Map<String, Object> flightMap = parseJson(flightJson);
                Long flightId = (Long) flightMap.get("id");
                int availableSeats = (int) flightMap.get("availableSeats");
                cancelFlight(flightId, availableSeats);
            }
            for (String hotelJson : bookedHotels) {
                Map<String, Object> hotelMap = parseJson(hotelJson);
                Long hotelId = (Long) hotelMap.get("id");
                int availableRooms = (int) hotelMap.get("availableRooms");
                cancelHotel(hotelId, availableRooms);
            }
            return false;
        }
    }

    public boolean clearTravelPackage(String userId) {
        for (TravelPackage travelPackage : travelPackages) {
            if (travelPackage.getUserId().equals(userId)) {
                travelPackage.clear();
            }
        }
        return true;
    }

    public boolean cancelTravelPackage(String userId) {
        for (TravelPackage travelPackage : travelPackages) {
            if (travelPackage.getUserId().equals(userId)) {
                for (String flightJson : travelPackage.getFlights()) {
                    Map<String, Object> flightMap = parseJson(flightJson);
                    Long flightId = (Long) flightMap.get("id");
                    int availableSeats = (int) flightMap.get("availableSeats");
                    cancelFlight(flightId, availableSeats);
                }
                for (String hotelJson : travelPackage.getHotels()) {
                    Map<String, Object> hotelMap = parseJson(hotelJson);
                    Long hotelId = (Long) hotelMap.get("id");
                    int availableRooms = (int) hotelMap.get("availableRooms");
                    cancelHotel(hotelId, availableRooms);
                }
                travelPackage.clear();
            }
        }
        return true;
    }

    public boolean cancelItemInTravelPackage(String userId, ItemType type, Long itemId) {
        TravelPackage travelPackage = getTravelPackage(userId);
        if (travelPackage == null) {
            return false;
        }

        if (type == ItemType.FLIGHT) {
            for (String flightJson : travelPackage.getFlights()) {
                Map<String, Object> flightMap = parseJson(flightJson);
                Long flightId = (Long) flightMap.get("id");
                if (flightId.equals(itemId)) {
                    int availableSeats = (int) flightMap.get("availableSeats");
                    cancelFlight(flightId, availableSeats);
                    travelPackage.removeFlight(flightJson);
                    return true;
                }
            }
        } else if (type == ItemType.HOTEL) {
            for (String hotelJson : travelPackage.getHotels()) {
                Map<String, Object> hotelMap = parseJson(hotelJson);
                Long hotelId = (Long) hotelMap.get("id");
                if (hotelId.equals(itemId)) {
                    int availableRooms = (int) hotelMap.get("availableRooms");
                    cancelHotel(hotelId, availableRooms);
                    travelPackage.removeHotel(hotelJson);
                    return true;
                }
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

    public String getHotel(Long id) throws IOException, ExecutionException, InterruptedException {
        return publishMessage("hotel-retrieve-requests", "{\"id\":" + id + "}");
    }

    public boolean bookHotel(Long hotelId, int rooms) {
        try {
            String bookingDetails = "{\"hotelId\":" + hotelId + ",\"rooms\":" + rooms + "}";
            publishMessage("hotel-booking-requests", bookingDetails);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public String getFlights() throws IOException, ExecutionException, InterruptedException {
        return publishMessage("flight-retrieve-requests", "{}");
    }

    public String getFlight(Long id) throws IOException, ExecutionException, InterruptedException {
        return publishMessage("flight-retrieve-requests", "{\"id\":" + id + "}");
    }

    public boolean bookFlight(Long flightId, int seats) {
        try {
            String bookingDetails = "{\"flightId\":" + flightId + ",\"seats\":" + seats + "}";
            publishMessage("flight-booking-requests", bookingDetails);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean cancelFlight(Long flightId, int seats) {
        try {
            String cancelDetails = "{\"flightId\":" + flightId + ",\"seats\":" + seats + "}";
            publishMessage("flight-cancel-requests", cancelDetails);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean cancelHotel(Long hotelId, int rooms) {
        try {
            String cancelDetails = "{\"hotelId\":" + hotelId + ",\"rooms\":" + rooms + "}";
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
}
