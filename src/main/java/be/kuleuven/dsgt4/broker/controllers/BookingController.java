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
@RequestMapping("/api/bookings")
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
