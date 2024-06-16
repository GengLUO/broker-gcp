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
import com.google.protobuf.Api;
import com.google.api.core.ApiFutureCallback;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
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
     */
@RestController
@RequestMapping("/api/bookings")
public class BrokerRestController {

    private final BrokerService brokerService;
    private final TransactionCoordinatorService transactionCoordinatorService;
    private static final Executor executor = Executors.newCachedThreadPool();

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
    @PostMapping("/packages")
    public ResponseEntity<?> createTravelPackage(@RequestBody Map<String, Object> packageDetails) {
        try {
            String userId = (String) packageDetails.get("userId");
            TravelPackage travelPackage = transactionCoordinatorService.createTravelPackage(userId);
            packageDetails.put("packageId", travelPackage.getPackageId());
            // packageDetails.put("userId", userId);

            // Include packageId in the resource
            EntityModel<Map<String, String>> resource = EntityModel.of(
                    Map.of(
                            "message", "Travel package created successfully.",
                            "packageId", travelPackage.getPackageId()
                    ),
                    WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(BrokerRestController.class).createTravelPackage(packageDetails)).withSelfRel()
            );

            return ResponseEntity.status(HttpStatus.CREATED).body(resource);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to create travel package: " + e.getMessage());
        }
    }

    @PostMapping("/packages/{packageId}/flights")
    public ResponseEntity<?> addFlightToTravelPackage(@PathVariable String packageId, @RequestBody Map<String, Object> flightDetails) {
        try {
            transactionCoordinatorService.addFlightToPackage(packageId, flightDetails);
            return ResponseEntity.ok("Flight added to travel package.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to add flight to travel package: " + e.getMessage());
        }
    }

    @DeleteMapping("/packages/{packageId}/flights/{flightId}")
    public ResponseEntity<?> removeFlightFromTravelPackage(@PathVariable String packageId, @PathVariable String flightId) {
        try {
            transactionCoordinatorService.removeFlightFromPackage(packageId, flightId);
            return ResponseEntity.ok("Flight removed from travel package.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to remove flight from travel package: " + e.getMessage());
        }
    }

    @PostMapping("/packages/{packageId}/hotels")
    public ResponseEntity<?> addHotelToTravelPackage(@PathVariable String packageId, @RequestBody Map<String, Object> hotelDetails) {
        try {
            transactionCoordinatorService.addHotelToPackage(packageId, hotelDetails);
            return ResponseEntity.ok("Hotel added to travel package.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to add hotel to travel package: " + e.getMessage());
        }
    }

    @DeleteMapping("/packages/{packageId}/hotels/{hotelId}")
    public ResponseEntity<?> removeHotelFromTravelPackage(@PathVariable String packageId, @PathVariable String hotelId) {
        try {
            transactionCoordinatorService.removeHotelFromPackage(packageId, hotelId);
            return ResponseEntity.ok("Hotel removed from travel package.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to remove hotel from travel package: " + e.getMessage());
        }
    }

    @PostMapping("/packages/{packageId}/customers")
    public ResponseEntity<?> addCustomerToTravelPackage(@PathVariable String packageId, @RequestBody Map<String, Object> customerDetails) {
        try {
            transactionCoordinatorService.addCustomerToPackage(packageId, customerDetails);
            return ResponseEntity.ok("Customer added to travel package.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to add customer to travel package: " + e.getMessage());
        }
    }

    @DeleteMapping("/packages/{packageId}/customers/{customerId}")
    public ResponseEntity<?> removeCustomerFromTravelPackage(@PathVariable String packageId, @PathVariable String customerId) {
        try {
            transactionCoordinatorService.removeCustomerFromPackage(packageId, customerId);
            return ResponseEntity.ok("Customer removed from travel package.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to remove customer from travel package: " + e.getMessage());
        }
    }

    // Booking Methods: 2PC transaction preparation
    @PostMapping("/packages/{packageId}/book")
    public ResponseEntity<?> bookTravelPackage(@PathVariable String packageId, @RequestBody Map<String, Object> bookingDetails) {
        ApiFuture<String> result = transactionCoordinatorService.bookTravelPackage(packageId, bookingDetails);
        ApiFutures.addCallback(result, new ApiFutureCallback<String>() {
            @Override
            public void onFailure(Throwable t) {
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error processing travel package booking: " + t.getMessage());
            }

            @Override
            public void onSuccess(String messageId) {
                EntityModel<String> resource = bookingToEntityModel(packageId, bookingDetails.toString());
                ResponseEntity.created(WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(BrokerRestController.class)
                        .bookTravelPackage(packageId, bookingDetails)).toUri()).body(resource);
            }
        }, Runnable::run);
        return ResponseEntity.ok("Booking process initiated package ID: " + packageId);
    }

    // Booking Methods: 2PC transaction execution (check the confirmation of flight and hotel booking)
    @PostMapping("/packages/{packageId}/confirmFlight")
    public ResponseEntity<String> confirmFlightBooking(@RequestBody String packageId) {
        try {
            ApiFuture<String> result = transactionCoordinatorService.checkBookingConfirmation(packageId, "flight");
            ApiFutures.addCallback(result, new ApiFutureCallback<String>() {
                @Override
                public void onFailure(Throwable t) {
                    ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error processing flight booking confirmation: " + t.getMessage());
                }

                @Override
                public void onSuccess(String message) {
                    ResponseEntity.ok("Flight booking confirmed for package ID: " + packageId);
                }
            }, executor);
            return ResponseEntity.ok("Flight booking confirmed for package ID: " + packageId);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to confirm flight booking: " + e.getMessage());
        }
    }

    @PostMapping("/packages/{packageId}/confirmHotel")
    public ResponseEntity<String> confirmHotelBooking(@RequestBody String packageId) {
        try {
            ApiFuture<String> result = transactionCoordinatorService.checkBookingConfirmation(packageId, "hotel");
            ApiFutures.addCallback(result, new ApiFutureCallback<String>() {
                @Override
                public void onFailure(Throwable t) {
                    ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error processing hotel booking confirmation: " + t.getMessage());
                }

                @Override
                public void onSuccess(String message) {
                    ResponseEntity.ok("Hotel booking confirmed for package ID: " + packageId);
                }
            }, executor);
            return ResponseEntity.ok("Hotel booking confirmed for package ID: " + packageId);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to confirm hotel booking: " + e.getMessage());
        }
    }

    // Booking Methods: 2PC transaction execution
    @PostMapping("/packages/{packageId}/confirm")
    public ResponseEntity<String> confrimTravelPackageBooking(@RequestBody Map<String, Object> bookingDetails) {
        String packageId = (String) bookingDetails.get("packageId");
        ApiFuture<String> result = transactionCoordinatorService.confirmTravelPackage(packageId, bookingDetails);
        ApiFutures.addCallback(result, new ApiFutureCallback<String>() {
            @Override
            public void onFailure(Throwable t) {
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error processing travel package booking confirmation: " + t.getMessage());
            }

            @Override
            public void onSuccess(String messageId) {
                ResponseEntity.ok("Travel package booking confirmed.");
            }
        }, Runnable::run);
        return ResponseEntity.ok("Booking confirmation process initiated for package ID: " + packageId);
    }


    // Booking Methods: 2PC transaction execution (abort)
    @DeleteMapping("/packages/{packageId}/cancel")
    public ResponseEntity<?> cancelTravelPackage(@PathVariable String packageId) {
        ApiFuture<String> result = transactionCoordinatorService.cancelTravelPackage(packageId);
        ApiFutures.addCallback(result, new ApiFutureCallback<String>() {
            @Override
            public void onFailure(Throwable t) {
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error processing travel package cancellation: " + t.getMessage());
            }

            @Override
            public void onSuccess(String messageId) {
                ResponseEntity.ok("Travel package cancelled for package ID: " + packageId);
            }
        }, Runnable::run);
        return null;
    }

    // After Booking Methods
    @PutMapping("/packages/{packageId}/flights")
    public ResponseEntity<?> updateFlightInTravelPackage(@PathVariable String packageId, @RequestBody Map<String, Object> flightDetails) {
        try {
            transactionCoordinatorService.updateFlightInPackage(packageId, flightDetails);
            return ResponseEntity.ok("Flight updated in travel package.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to update flight in travel package: " + e.getMessage());
        }
    }

    @PutMapping("/packages/{packageId}/hotels")
    public ResponseEntity<?> updateHotelInTravelPackage(@PathVariable String packageId, @RequestBody Map<String, Object> hotelDetails) {
        try {
            transactionCoordinatorService.updateHotelInPackage(packageId, hotelDetails);
            return ResponseEntity.ok("Hotel updated in travel package.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to update hotel in travel package: " + e.getMessage());
        }
    }

    @PutMapping("/packages/{packageId}/customers")
    public ResponseEntity<?> updateCustomerInTravelPackage(@PathVariable String packageId, @RequestBody Map<String, Object> customerDetails) {
        try {
            transactionCoordinatorService.updateCustomerInPackage(packageId, customerDetails);
            return ResponseEntity.ok("Customer updated in travel package.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to update customer in travel package: " + e.getMessage());
        }
    }

    private EntityModel<String> bookingToEntityModel(String packageId, String message) {
        return EntityModel.of(message,
                WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(BrokerRestController.class).bookTravelPackage(packageId, new HashMap<>())).withSelfRel(),
                WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(BrokerRestController.class).createTravelPackage(new HashMap<>())).withRel("create-package"),
                WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(BrokerRestController.class).cancelTravelPackage(packageId)).withRel("cancel-package"),
                WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(BrokerRestController.class).addFlightToTravelPackage(packageId, new HashMap<>())).withRel("add-flight"),
                WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(BrokerRestController.class).removeFlightFromTravelPackage(packageId, "")).withRel("remove-flight"),
                WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(BrokerRestController.class).addHotelToTravelPackage(packageId, new HashMap<>())).withRel("add-hotel"),
                WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(BrokerRestController.class).removeHotelFromTravelPackage(packageId, "")).withRel("remove-hotel"),
                WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(BrokerRestController.class).addCustomerToTravelPackage(packageId, new HashMap<>())).withRel("add-customer"),
                WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(BrokerRestController.class).removeCustomerFromTravelPackage(packageId, "")).withRel("remove-customer"),
                WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(BrokerRestController.class).updateFlightInTravelPackage(packageId, new HashMap<>())).withRel("update-flight"),
                WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(BrokerRestController.class).updateHotelInTravelPackage(packageId, new HashMap<>())).withRel("update-hotel"),
                WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(BrokerRestController.class).updateCustomerInTravelPackage(packageId, new HashMap<>())).withRel("update-customer")
        );
    }
}