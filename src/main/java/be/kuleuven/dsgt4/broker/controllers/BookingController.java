//TODO:
// HATEOAS -> use WebMvcLinkBuilder. (not CollectionModel) -> DONE
// Integration with Authentication -> is it needed? is so, check firestore controller for usage example
// Error Handling,
// Data validation

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

    @PostMapping("/{userId}")
    public ResponseEntity<?> createTravelPackage(@PathVariable String userId) {
        TravelPackage travelPackage = publisherService.createTravelPackage(userId);
        EntityModel<String> resource = bookingToEntityModel(travelPackage.getUserId(), "Travel package created successfully.");
        return ResponseEntity.status(HttpStatus.CREATED).body(resource);
    }

    @PostMapping("/{userId}/flights")
    public ResponseEntity<?> addFlightToTravelPackage(@PathVariable String userId, @RequestBody String flightJson) {
        TravelPackage travelPackage = publisherService.getTravelPackage(userId);
        if (travelPackage == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Travel package not found for user ID: " + userId);
        }
        publisherService.addFlightToTravelPackage(travelPackage, flightJson);
        return ResponseEntity.ok("Flight added to travel package for user ID: " + userId);
    }

    @PostMapping("/{userId}/hotels")
    public ResponseEntity<?> addHotelToTravelPackage(@PathVariable String userId, @RequestBody String hotelJson) {
        TravelPackage travelPackage = publisherService.getTravelPackage(userId);
        if (travelPackage == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Travel package not found for user ID: " + userId);
        }
        publisherService.addHotelToTravelPackage(travelPackage, hotelJson);
        return ResponseEntity.ok("Hotel added to travel package for user ID: " + userId);
    }

    @DeleteMapping("/{userId}/flights")
    public ResponseEntity<?> removeFlightFromTravelPackage(@PathVariable String userId, @RequestBody String flightJson) {
        TravelPackage travelPackage = publisherService.getTravelPackage(userId);
        if (travelPackage == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Travel package not found for user ID: " + userId);
        }
        publisherService.removeFlightFromTravelPackage(travelPackage, flightJson);
        return ResponseEntity.ok("Flight removed from travel package for user ID: " + userId);
    }

    @DeleteMapping("/{userId}/hotels")
    public ResponseEntity<?> removeHotelFromTravelPackage(@PathVariable String userId, @RequestBody String hotelJson) {
        TravelPackage travelPackage = publisherService.getTravelPackage(userId);
        if (travelPackage == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Travel package not found for user ID: " + userId);
        }
        publisherService.removeHotelFromTravelPackage(travelPackage, hotelJson);
        return ResponseEntity.ok("Hotel removed from travel package for user ID: " + userId);
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
    public ResponseEntity<?> getHotel(@PathVariable Long id) {
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
    public ResponseEntity<?> getFlight(@PathVariable Long id) {
        try {
            String flight = publisherService.getFlight(id);
            return ResponseEntity.ok(flight);
        } catch (IOException | ExecutionException | InterruptedException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to retrieve flight: " + e.getMessage());
        }
    }

    @PostMapping("/hotels")
    public ResponseEntity<?> createHotelBooking(@RequestBody Map<String, Object> bookingDetails) {
        try {
            String messageId = publisherService.publishMessage("hotel-booking-requests", bookingDetails);
            EntityModel<String> resource = bookingToEntityModel("hotel", messageId, bookingDetails.toString());
            return ResponseEntity.created(WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(BookingController.class)
                    .createHotelBooking(bookingDetails)).toUri()).body(resource);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error processing hotel booking: " + e.getMessage());
        }
    }

    @PostMapping("/flights")
    public ResponseEntity<?> createFlightBooking(@RequestBody Map<String, Object> bookingDetails) {
        try {
            String messageId = publisherService.publishMessage("flight-booking-requests", bookingDetails);
            EntityModel<String> resource = bookingToEntityModel("flight", messageId, bookingDetails.toString());
            return ResponseEntity.created(WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(BookingController.class)
                    .createFlightBooking(bookingDetails)).toUri()).body(resource);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error processing flight booking: " + e.getMessage());
        }
    }

    @PostMapping("/book")
    public ResponseEntity<?> bookTravelPackage(@RequestBody Map<String, Object> bookingDetails) {
        try {
            String packageId = (String) bookingDetails.get("packageId");
            String messageId = transactionCoordinatorService.bookTravelPackage(packageId, bookingDetails).get();
            EntityModel<String> resource = bookingToEntityModel("travelPackage", messageId, bookingDetails.toString());
            return ResponseEntity.created(WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(BookingController.class)
                    .bookTravelPackage(bookingDetails)).toUri()).body(resource);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error processing travel package booking: " + e.getMessage());
        }
    }

    @PutMapping("/{type}/{id}")
    public ResponseEntity<?> updateBooking(@PathVariable String type, @PathVariable String id, @RequestBody Map<String, Object> updateDetails) {
        try {
            transactionCoordinatorService.updateTravelPackage(id, updateDetails).get();
            return ResponseEntity.ok("Booking updated successfully.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error updating booking: " + e.getMessage());
        }
    }

    @DeleteMapping("/{type}/{id}")
    public ResponseEntity<?> deleteBooking(@PathVariable String type, @PathVariable String id) {
        try {
            transactionCoordinatorService.deleteTravelPackage(id).get();
            return ResponseEntity.ok("Booking deleted successfully.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error deleting booking: " + e.getMessage());
        }
    }
    
    @GetMapping("/{type}/{id}")
    public ResponseEntity<?> retrieveBooking(@PathVariable String type, @PathVariable String id) {
        // Simulate fetching booking details, actual implementation needed
        String bookingDetails = "Booking details for " + type + " ID: " + id;
        return ResponseEntity.ok(bookingToEntityModel(type, id, bookingDetails));
    }

    @DeleteMapping("/{type}/{id}")
    public ResponseEntity<?> cancelBooking(@PathVariable String type, @PathVariable String id) {
        // Simulate booking cancellation, actual implementation needed
        return ResponseEntity.ok(type.toUpperCase() + " booking cancelled for ID: " + id);
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<?> cancelTravelPackage(@PathVariable String userId) {
        boolean success = publisherService.cancelTravelPackage(userId);
        if (success) {
            return ResponseEntity.ok("Travel package cancelled for user ID: " + userId);
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to cancel travel package for user ID: " + userId);
        }
    }

    @DeleteMapping("/{userId}/{type}/{itemId}")
    public ResponseEntity<?> cancelItemInTravelPackage(@PathVariable String userId, @PathVariable String type, @PathVariable Long itemId) {
        try {
            ItemType itemType = ItemType.fromString(type);
            boolean success = publisherService.cancelItemInTravelPackage(userId, itemType, itemId);
            if (success) {
                return ResponseEntity.ok(itemType.getType() + " cancelled for item ID: " + itemId + " in user ID: " + userId);
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to cancel " + itemType.getType() + " for item ID: " + itemId + " in user ID: " + userId);
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @GetMapping("/{userId}")
    public ResponseEntity<?> getTravelPackage(@PathVariable String userId) {
        TravelPackage travelPackage = publisherService.getTravelPackage(userId);
        if (travelPackage == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Travel package not found for user ID: " + userId);
        }
        return ResponseEntity.ok(travelPackage);
    }

    private EntityModel<String> bookingToEntityModel(String type, String messageId, String bookingDetails) {
        return EntityModel.of("Booking request submitted successfully. Message ID: " + messageId,
                WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(BookingController.class).retrieveBooking(type, messageId)).withSelfRel(),
                WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(BookingController.class).cancelBooking(type, messageId)).withRel("cancel-booking")
        );
    }
}
