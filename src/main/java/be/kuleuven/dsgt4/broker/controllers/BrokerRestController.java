package be.kuleuven.dsgt4.broker.controllers;

import be.kuleuven.dsgt4.auth.WebSecurityConfig;
import be.kuleuven.dsgt4.broker.domain.TravelPackage;
import be.kuleuven.dsgt4.broker.services.BrokerService;
import be.kuleuven.dsgt4.broker.services.TransactionCoordinatorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AuthorizationServiceException;
import org.springframework.web.bind.annotation.*;
import com.google.api.core.ApiFuture;
import com.google.api.core.ApiFutures;
import com.google.api.core.ApiFutureCallback;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/bookings")
public class BrokerRestController {
    /** Documentation
     * This class is responsible for handling HTTP requests related to travel bookings.
     * Before Booking:
     *  Operations that modify the travel package before booking do not need to involve the suppliers directly. These operations typically involve preparing the package with user preferences and initial selections.
     * Relevant Methods:
     *  - createTravelPackage: Create a new travel package.
     *  - addFlightToTravelPackage: Add a flight to the travel package.
     *  - removeFlightFromTravelPackage: Remove a flight from the travel package.
     *  - addHotelToTravelPackage: Add a hotel to the travel package.
     *  - removeHotelFromTravelPackage: Remove a hotel from the travel package.
     *  - addCustomerToTravelPackage: Add a customer to the travel package.
     *  - removeCustomerFromTravelPackage: Remove a customer from the travel package.
     * Booking:
     * Operations that book the travel package involve transactional operations to ensure consistency. These operations involve communication with the suppliers to book the selected flights and hotels.
     * Relevant Methods:
     * - bookTravelPackage: Book the travel package.
     * After Booking:
     *  Operations that modify the travel package after booking must be communicated and coordinated with the suppliers. These operations involve transactional operations to ensure consistency.
     * Relevant Methods:
     * - cancelTravelPackage: Cancel the travel package.
     * - updateFlightInTravelPackage: Update the flight in the travel package.
     * - updateHotelInTravelPackage: Update the hotel in the travel package.
     * - updateCustomerInTravelPackage: Update the customer in the travel package.
     * Pub/Sub Push:
     * Methods to handle Pub/Sub push messages from the suppliers.
     * Relevant Methods:
     * - handleHotelPubSubPush: Handle a push message from the hotel supplier.
     * - handleFlightPubSubPush: Handle a push message from the flight supplier.
     */
    private final BrokerService brokerService;
    private final TransactionCoordinatorService transactionCoordinatorService;

    @Autowired
    public BrokerRestController(BrokerService brokerService, TransactionCoordinatorService transactionCoordinatorService) {
        this.brokerService = brokerService;
        this.transactionCoordinatorService = transactionCoordinatorService;
    }

    @GetMapping("/test-publish/{topic}")
    public ResponseEntity<?> testPublish(@PathVariable String topic) {
        try {
            Map<String, Object> testMessage = new HashMap<>();
            testMessage.put("type", "test");
            testMessage.put("message", "Test message");
            System.out.println("Finish creating test message");
            String messageId = brokerService.publishMessage(topic, testMessage);
            return ResponseEntity.ok("Test message published successfully. Message ID: " + messageId);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to publish test message: " + e.getMessage());
        }
    }

    //test function to test client can send message to broker
    //then, broker can send to the pub/sub
    @GetMapping("/emulator-publish/{project}/{topic}/{message}")
    public ResponseEntity<?> emulatorPublish(@PathVariable String project, @PathVariable String topic, @PathVariable String message) {
        try {
            System.out.println("Inside emulatorPublish" + project + topic + message);
            String messageId = brokerService.emulatorPublishMessage(project,topic,message);
            return ResponseEntity.ok("Test message published successfully. Message ID: " + messageId);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to publish test message: " + e.getMessage());
        }
    }

    // Before Booking Methods
    @PostMapping("/{userId}/packages")
    public ResponseEntity<?> createTravelPackage(@PathVariable String userId, @RequestBody Map<String, Object> packageDetails) {
        try {
            TravelPackage travelPackage = transactionCoordinatorService.createTravelPackage(userId);
            packageDetails.put("packageId", travelPackage.getPackageId());
            packageDetails.put("userId", userId);

//            String flightMessageId = brokerService.publishMessage("flight-booking-requests", packageDetails);
//            String hotelMessageId = brokerService.publishMessage("hotel-booking-requests", packageDetails);

//            EntityModel<String> resource = bookingToEntityModel(userId, travelPackage.getPackageId(), "Travel package created successfully.");

            // Include packageId in the resource
            EntityModel<Map<String, String>> resource = EntityModel.of(
                    Map.of(
                            "message", "Travel package created successfully.",
                            "packageId", travelPackage.getPackageId()
                    ),
                    WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(BrokerRestController.class).createTravelPackage(userId, packageDetails)).withSelfRel()
            );

            return ResponseEntity.status(HttpStatus.CREATED).body(resource);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to create travel package: " + e.getMessage());
        }
    }

    @PostMapping("/{userId}/packages/{packageId}/flights")
    public ResponseEntity<?> addFlightToTravelPackage(@PathVariable String userId, @PathVariable String packageId, @RequestBody Map<String, Object> flightDetails) {
        try {
            transactionCoordinatorService.addFlightToPackage(userId, packageId, flightDetails);
            return ResponseEntity.ok("Flight added to travel package.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to add flight to travel package: " + e.getMessage());
        }
    }

    @DeleteMapping("/{userId}/packages/{packageId}/flights/{flightId}")
    public ResponseEntity<?> removeFlightFromTravelPackage(@PathVariable String userId, @PathVariable String packageId, @PathVariable String flightId) {
        try {
            transactionCoordinatorService.removeFlightFromPackage(userId, packageId, flightId);
            return ResponseEntity.ok("Flight removed from travel package.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to remove flight from travel package: " + e.getMessage());
        }
    }

    @PostMapping("/{userId}/packages/{packageId}/hotels")
    public ResponseEntity<?> addHotelToTravelPackage(@PathVariable String userId, @PathVariable String packageId, @RequestBody Map<String, Object> hotelDetails) {
        try {
            transactionCoordinatorService.addHotelToPackage(userId, packageId, hotelDetails);
            return ResponseEntity.ok("Hotel added to travel package.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to add hotel to travel package: " + e.getMessage());
        }
    }

    @DeleteMapping("/{userId}/packages/{packageId}/hotels/{hotelId}")
    public ResponseEntity<?> removeHotelFromTravelPackage(@PathVariable String userId, @PathVariable String packageId, @PathVariable String hotelId) {
        try {
            transactionCoordinatorService.removeHotelFromPackage(userId, packageId, hotelId);
            return ResponseEntity.ok("Hotel removed from travel package.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to remove hotel from travel package: " + e.getMessage());
        }
    }

    @PostMapping("/{userId}/packages/{packageId}/customers")
    public ResponseEntity<?> addCustomerToTravelPackage(@PathVariable String userId, @PathVariable String packageId, @RequestBody Map<String, Object> customerDetails) {
        try {
            transactionCoordinatorService.addCustomerToPackage(userId, packageId, customerDetails);
            return ResponseEntity.ok("Customer added to travel package.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to add customer to travel package: " + e.getMessage());
        }
    }

    @DeleteMapping("/{userId}/packages/{packageId}/customers/{customerId}")
    public ResponseEntity<?> removeCustomerFromTravelPackage(@PathVariable String userId, @PathVariable String packageId, @PathVariable String customerId) {
        try {
            transactionCoordinatorService.removeCustomerFromPackage(userId, packageId, customerId);
            return ResponseEntity.ok("Customer removed from travel package.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to remove customer from travel package: " + e.getMessage());
        }
    }

    // Booking Method
    @PostMapping("/{userId}/packages/{packageId}/book")
    public ResponseEntity<?> bookTravelPackage(@PathVariable String userId, @PathVariable String packageId, @RequestBody Map<String, Object> bookingDetails) {
        ApiFuture<String> result = transactionCoordinatorService.bookTravelPackage(packageId, bookingDetails);
        ApiFutures.addCallback(result, new ApiFutureCallback<String>() {
            @Override
            public void onFailure(Throwable t) {
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error processing travel package booking: " + t.getMessage());
            }

            @Override
            public void onSuccess(String messageId) {
                EntityModel<String> resource = bookingToEntityModel(userId, packageId, bookingDetails.toString());
                ResponseEntity.created(WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(BrokerRestController.class)
                        .bookTravelPackage(userId, packageId, bookingDetails)).toUri()).body(resource);
            }
        }, Runnable::run);
        return ResponseEntity.ok("Booking process initiated for user ID: " + userId + " and package ID: " + packageId);
    }

    // After Booking Methods
    @PutMapping("/{userId}/packages/{packageId}/flights")
    public ResponseEntity<?> updateFlightInTravelPackage(@PathVariable String userId, @PathVariable String packageId, @RequestBody Map<String, Object> flightDetails) {
        try {
            transactionCoordinatorService.updateFlightInPackage(userId, packageId, flightDetails);
            brokerService.publishMessage("flight-update-requests", flightDetails);
            return ResponseEntity.ok("Flight updated in travel package.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to update flight in travel package: " + e.getMessage());
        }
    }

    @PutMapping("/{userId}/packages/{packageId}/hotels")
    public ResponseEntity<?> updateHotelInTravelPackage(@PathVariable String userId, @PathVariable String packageId, @RequestBody Map<String, Object> hotelDetails) {
        try {
            transactionCoordinatorService.updateHotelInPackage(userId, packageId, hotelDetails);
            brokerService.publishMessage("hotel-update-requests", hotelDetails);
            return ResponseEntity.ok("Hotel updated in travel package.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to update hotel in travel package: " + e.getMessage());
        }
    }

    @PutMapping("/{userId}/packages/{packageId}/customers")
    public ResponseEntity<?> updateCustomerInTravelPackage(@PathVariable String userId, @PathVariable String packageId, @RequestBody Map<String, Object> customerDetails) {
        try {
            transactionCoordinatorService.updateCustomerInPackage(userId, packageId, customerDetails);
            return ResponseEntity.ok("Customer updated in travel package.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to update customer in travel package: " + e.getMessage());
        }
    }

    @DeleteMapping("/{userId}/packages/{packageId}")
    public ResponseEntity<?> cancelTravelPackage(@PathVariable String userId, @PathVariable String packageId) {
        ApiFuture<Void> result = transactionCoordinatorService.cancelTravelPackage(userId, packageId);
        ApiFutures.addCallback(result, new ApiFutureCallback<Void>() {
            @Override
            public void onFailure(Throwable t) {
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to cancel travel package for user ID: " + userId + " and package ID: " + packageId);
            }

            @Override
            public void onSuccess(Void v) {
                ResponseEntity.ok("Travel package cancelled for user ID: " + userId + " and package ID: " + packageId);
            }
        }, Runnable::run);
        return ResponseEntity.ok("Cancellation process initiated for user ID: " + userId + " and package ID: " + packageId);
    }

    @PostMapping("/hotel/pubsub/push")
    public ResponseEntity<String> handleHotelPubSubPush(@RequestBody Map<String, Object> message) {
        return handlePubSubPush(message, "hotel");
    }

    @PostMapping("/flight/pubsub/push")
    public ResponseEntity<String> handleFlightPubSubPush(@RequestBody Map<String, Object> message) {
        return handlePubSubPush(message, "flight");
    }

    private ResponseEntity<String> handlePubSubPush(Map<String, Object> message, String type) {
        String packageId = (String) message.get("packageId");
        String userId = (String) message.get("userId");
        Long id = Long.valueOf(message.get(type + "Id").toString());
        int booked = (int) message.get(type + "sBooked");
        boolean success = (boolean) message.get("success");

        // Process the response
        System.out.println(type + " booking response: " + message);

        // Here you can add logic to update Firestore or perform other actions based on the response

        return ResponseEntity.ok("Message processed");
    }

    private EntityModel<String> bookingToEntityModel(String userId, String packageId, String message) {
        return EntityModel.of(message,
                WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(BrokerRestController.class).bookTravelPackage(userId, packageId, new HashMap<>())).withSelfRel(),
                WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(BrokerRestController.class).createTravelPackage(userId, new HashMap<>())).withRel("create-package"),
                WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(BrokerRestController.class).cancelTravelPackage(userId, packageId)).withRel("cancel-package"),
                WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(BrokerRestController.class).addFlightToTravelPackage(userId, packageId, new HashMap<>())).withRel("add-flight"),
                WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(BrokerRestController.class).removeFlightFromTravelPackage(userId, packageId, "")).withRel("remove-flight"),
                WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(BrokerRestController.class).addHotelToTravelPackage(userId, packageId, new HashMap<>())).withRel("add-hotel"),
                WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(BrokerRestController.class).removeHotelFromTravelPackage(userId, packageId, "")).withRel("remove-hotel"),
                WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(BrokerRestController.class).addCustomerToTravelPackage(userId, packageId, new HashMap<>())).withRel("add-customer"),
                WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(BrokerRestController.class).removeCustomerFromTravelPackage(userId, packageId, "")).withRel("remove-customer"),
                WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(BrokerRestController.class).updateFlightInTravelPackage(userId, packageId, new HashMap<>())).withRel("update-flight"),
                WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(BrokerRestController.class).updateHotelInTravelPackage(userId, packageId, new HashMap<>())).withRel("update-hotel"),
                WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(BrokerRestController.class).updateCustomerInTravelPackage(userId, packageId, new HashMap<>())).withRel("update-customer")
        );
    }

    /*******************new methods******************/
//    @GetMapping("/flights")
//    public ResponseEntity<?> getFlights() {
//        try {
//            Map<String, Object> message = new HashMap<>();
//            message.put("type", "flight-info-requests");
//            message.put("action", "getAllFlights");
//            String correlationId = UUID.randomUUID().toString();
//            message.put("correlationId", correlationId);
//
//            // Publish request message
//            brokerService.publishMessage("flight-info-requests", message);
//
//            // Wait for the response (using a blocking queue for simplicity)
//            BlockingQueue<String> responseQueue = new ArrayBlockingQueue<>(1);
//            brokerService.subscribeToResponse(correlationId, responseQueue);
//
//            // Get the response
//            String flightsJson = responseQueue.poll(30, TimeUnit.SECONDS); // Wait for 30 seconds
//
//            if (flightsJson == null) {
//                return ResponseEntity.status(HttpStatus.REQUEST_TIMEOUT).body("No response received for flight info request.");
//            }
//
//            return ResponseEntity.ok(flightsJson);
//        } catch (Exception e) {
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to get flights: " + e.getMessage());
//        }
//    }

//    @GetMapping("/flights")
//    public ResponseEntity<?> getFlights() {
//        try {
//            Map<String, Object> message = new HashMap<>();
//            message.put("type", "flight-info-requests");
//            message.put("action", "getAllFlights");
//
//            String messageId = brokerService.publishMessage("flight-info-requests", message);
//            return ResponseEntity.ok("Request to get all flights has been published. Message ID: " + messageId);
//        } catch (Exception e) {
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to publish message to get flights: " + e.getMessage());
//        }
//    }
}

/**
package be.kuleuven.dsgt4.broker.controllers;

import be.kuleuven.dsgt4.broker.domain.ItemType;
import be.kuleuven.dsgt4.broker.domain.TravelPackage;
import be.kuleuven.dsgt4.broker.services.BrokerPublisherService;
import be.kuleuven.dsgt4.broker.services.TransactionCoordinatorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;

@RestController
@RequestMapping("/bookings")
public class BookingController {

    private final BrokerPublisherService publisherService;
    private final TransactionCoordinatorService transactionCoordinatorService;

    @Autowired
    public BookingController(BrokerPublisherService publisherService, TransactionCoordinatorService transactionCoordinatorService) {
        this.publisherService = publisherService;
        this.transactionCoordinatorService = transactionCoordinatorService;
    }

    @GetMapping("/test-publish/{topic}")
    public ResponseEntity<?> testPublish(@PathVariable String topic) {
        try {
            Map<String, Object> testMessage = new HashMap<>();
            testMessage.put("type", "test");
            testMessage.put("message", "Test message");
            String messageId = publisherService.publishMessage(topic, testMessage);
            return ResponseEntity.ok("Test message published successfully. Message ID: " + messageId);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to publish test message: " + e.getMessage());
        }
    }

    @GetMapping("/hotels")
    public ResponseEntity<?> getHotels() {
        try {
            String hotels = publisherService.getHotels();
            return ResponseEntity.ok(hotels);
        } catch (IOException | ExecutionException | InterruptedException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to retrieve hotels: " + e.getMessage());
        }
    }

    @GetMapping("/hotels/{id}")
    public ResponseEntity<?> getHotel(@PathVariable String id) {
        try {
            String hotel = publisherService.getHotel(id);
            return ResponseEntity.ok(hotel);
        } catch (IOException | ExecutionException | InterruptedException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to retrieve hotel: " + e.getMessage());
        }
    }

    @GetMapping("/flights")
    public ResponseEntity<?> getFlights() {
        try {
            String flights = publisherService.getFlights();
            return ResponseEntity.ok(flights);
        } catch (IOException | ExecutionException | InterruptedException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to retrieve flights: " + e.getMessage());
        }
    }

    @GetMapping("/flights/{id}")
    public ResponseEntity<?> getFlight(@PathVariable String id) {
        try {
            String flight = publisherService.getFlight(id);
            return ResponseEntity.ok(flight);
        } catch (IOException | ExecutionException | InterruptedException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to retrieve flight: " + e.getMessage());
        }
    }

    @PostMapping("/{userId}/packages")
    public ResponseEntity<?> createTravelPackage(@PathVariable String userId) {
        TravelPackage travelPackage = publisherService.createTravelPackage(userId);
        EntityModel<String> resource = bookingToEntityModel(userId, travelPackage.getPackageId(), "Travel package created successfully.");
        return ResponseEntity.status(HttpStatus.CREATED).body(resource);
    }

    @PostMapping("/{userId}/packages/{packageId}/flights")
    public ResponseEntity<?> addFlightToTravelPackage(@PathVariable String userId, @PathVariable String packageId, @RequestBody String flightJson) {
        TravelPackage travelPackage = publisherService.getTravelPackage(userId, packageId);
        if (travelPackage == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Travel package not found for user ID: " + userId + " and package ID: " + packageId);
        }
        publisherService.addFlightToTravelPackage(travelPackage, flightJson);
        return ResponseEntity.ok("Flight added to travel package for user ID: " + userId + " and package ID: " + packageId);
    }

    @DeleteMapping("/{userId}/packages/{packageId}/flights")
    public ResponseEntity<?> removeFlightFromTravelPackage(@PathVariable String userId, @PathVariable String packageId, @RequestBody String flightJson) {
        TravelPackage travelPackage = publisherService.getTravelPackage(userId, packageId);
        if (travelPackage == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Travel package not found for user ID: " + userId + " and package ID: " + packageId);
        }
        publisherService.removeFlightFromTravelPackage(travelPackage, flightJson);
        return ResponseEntity.ok("Flight removed from travel package for user ID: " + userId + " and package ID: " + packageId);
    }

    @PostMapping("/{userId}/packages/{packageId}/hotels")
    public ResponseEntity<?> addHotelToTravelPackage(@PathVariable String userId, @PathVariable String packageId, @RequestBody String hotelJson) {
        TravelPackage travelPackage = publisherService.getTravelPackage(userId, packageId);
        if (travelPackage == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Travel package not found for user ID: " + userId + " and package ID: " + packageId);
        }
        publisherService.addHotelToTravelPackage(travelPackage, hotelJson);
        return ResponseEntity.ok("Hotel added to travel package for user ID: " + userId + " and package ID: " + packageId);
    }

    @DeleteMapping("/{userId}/packages/{packageId}/hotels")
    public ResponseEntity<?> removeHotelFromTravelPackage(@PathVariable String userId, @PathVariable String packageId, @RequestBody String hotelJson) {
        TravelPackage travelPackage = publisherService.getTravelPackage(userId, packageId);
        if (travelPackage == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Travel package not found for user ID: " + userId + " and package ID: " + packageId);
        }
        publisherService.removeHotelFromTravelPackage(travelPackage, hotelJson);
        return ResponseEntity.ok("Hotel removed from travel package for user ID: " + userId + " and package ID: " + packageId);
    }

    @PostMapping("/{userId}/packages/{packageId}/book/hotels")
    public ResponseEntity<?> createHotelBooking(@PathVariable String userId, @PathVariable String packageId, @RequestBody Map<String, Object> bookingDetails) {
        try {
            String messageId = publisherService.publishMessage("hotel-booking-requests", bookingDetails);
            EntityModel<String> resource = bookingToEntityModel(userId, packageId, bookingDetails.toString());
            return ResponseEntity.created(WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(BookingController.class)
                    .createHotelBooking(userId, packageId, bookingDetails)).toUri()).body(resource);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error processing hotel booking: " + e.getMessage());
        }
    }

    @PostMapping("/{userId}/packages/{packageId}/book/flights")
    public ResponseEntity<?> createFlightBooking(@PathVariable String userId, @PathVariable String packageId, @RequestBody Map<String, Object> bookingDetails) {
        try {
            String messageId = publisherService.publishMessage("flight-booking-requests", bookingDetails);
            EntityModel<String> resource = bookingToEntityModel(userId, packageId, bookingDetails.toString());
            return ResponseEntity.created(WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(BookingController.class)
                    .createFlightBooking(userId, packageId, bookingDetails)).toUri()).body(resource);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error processing flight booking: " + e.getMessage());
        }
    }

    @PostMapping("/{userId}/packages/{packageId}/book")
    public ResponseEntity<?> bookTravelPackage(@PathVariable String userId, @PathVariable String packageId, @RequestBody Map<String, Object> bookingDetails) {
        try {
            String messageId = transactionCoordinatorService.bookTravelPackage(packageId, bookingDetails).get();
            EntityModel<String> resource = bookingToEntityModel(userId, packageId, bookingDetails.toString());
            return ResponseEntity.created(WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(BookingController.class)
                    .bookTravelPackage(userId, packageId, bookingDetails)).toUri()).body(resource);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error processing travel package booking: " + e.getMessage());
        }
    }

    @DeleteMapping("/{userId}/packages/{packageId}")
    public ResponseEntity<?> cancelTravelPackage(@PathVariable String userId, @PathVariable String packageId) {
        boolean success = publisherService.cancelTravelPackage(userId, packageId);
        if (success) {
            return ResponseEntity.ok("Travel package cancelled for user ID: " + userId + " and package ID: " + packageId);
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to cancel travel package for user ID: " + userId + " and package ID: " + packageId);
        }
    }

    @DeleteMapping("/{userId}/packages/{packageId}/items/{type}/{itemId}")
    public ResponseEntity<?> cancelItemInTravelPackage(@PathVariable String userId, @PathVariable String packageId, @PathVariable String type, @PathVariable String itemId) {
        try {
            ItemType itemType = ItemType.fromString(type); // item being flight or hotel
            boolean success = publisherService.cancelItemInTravelPackage(userId, packageId, itemType, itemId);
            if (success) {
                return ResponseEntity.ok(itemType.getType() + " cancelled for item ID: " + itemId + " in user ID: " + userId + " and package ID: " + packageId);
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to cancel " + itemType.getType() + " for item ID: " + itemId + " in user ID: " + userId + " and package ID: " + packageId);
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @GetMapping("/{userId}/packages/{packageId}")
    public ResponseEntity<?> getTravelPackage(@PathVariable String userId, @PathVariable String packageId) {
        TravelPackage travelPackage = publisherService.getTravelPackage(userId, packageId);
        if (travelPackage == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Travel package not found for user ID: " + userId + " and package ID: " + packageId);
        }
        return ResponseEntity.ok(travelPackage);
    }

    private EntityModel<String> bookingToEntityModel(String userId, String packageId, String message) {
        return EntityModel.of(message,
                WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(BookingController.class).getTravelPackage(userId, packageId)).withSelfRel(),
                WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(BookingController.class).createTravelPackage(userId)).withRel("create-package"),
                WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(BookingController.class).addFlightToTravelPackage(userId, packageId, "")).withRel("add-flight"),
                WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(BookingController.class).addHotelToTravelPackage(userId, packageId, "")).withRel("add-hotel"),
                WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(BookingController.class).cancelTravelPackage(userId, packageId)).withRel("cancel-package"),
                WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(BookingController.class).cancelItemInTravelPackage(userId, packageId, "flight", "")).withRel("cancel-flight"),
                WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(BookingController.class).cancelItemInTravelPackage(userId, packageId, "hotel", "")).withRel("cancel-hotel")
        );
    }
}
*/