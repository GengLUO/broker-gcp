//TODO:
// HATEOAS -> use WebMvcLinkBuilder. (not CollectionModel) -> DONE
// Integration with Authentication -> is it needed? is so, check firestore controller for usage example
// Error Handling,
// Data validation

package be.kuleuven.dsgt4.broker.controllers;

import be.kuleuven.dsgt4.broker.services.BrokerPublisherService;
import be.kuleuven.dsgt4.broker.services.TransactionCoordinatorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
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

    private EntityModel<String> bookingToEntityModel(String type, String messageId, String bookingDetails) {
        return EntityModel.of("Booking request submitted successfully. Message ID: " + messageId,
                WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(BookingController.class).retrieveBooking(type, messageId)).withSelfRel(),
                WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(BookingController.class).cancelBooking(type, messageId)).withRel("cancel-booking")
        );
    }
}
