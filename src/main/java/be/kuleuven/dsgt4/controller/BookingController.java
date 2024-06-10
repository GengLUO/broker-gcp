package be.kuleuven.dsgt4.controller;

import be.kuleuven.dsgt4.broker.BrokerPublisherService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
//@RequestMapping("/api/bookings")
@RequestMapping("/bookings")
//TODO:
// HATEOAS -> use WebMvcLinkBuilder. (not CollectionModel) -> DONE
// Integration with Authentication -> is it needed? is so, check firestore controller for usage example
// Error Handling,
// Data validation
public class BookingController {

    private final BrokerPublisherService publisherService;

    @Autowired
    public BookingController(BrokerPublisherService publisherService) {
        this.publisherService = publisherService;
    }

    // Dummy endpoint for testing message publishing
    @GetMapping("/test-publish/{topic}")
    public ResponseEntity<?> testPublish(@PathVariable String topic) {
        try {
            return ResponseEntity.ok("Test message published successfully. topic: " + topic);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to publish test message: " + e.getMessage());
        }
    }

    // Post a new hotel booking
//    @PostMapping("/hotels")
//    public ResponseEntity<?> createHotelBooking(@RequestBody String bookingDetails) {
//        try {
//            String messageId = publisherService.publishMessage("hotel-booking-requests", bookingDetails);
//            return ResponseEntity.status(HttpStatus.CREATED).body("Hotel booking request submitted successfully. Message ID: " + messageId);
//        } catch (Exception e) {
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error processing hotel booking: " + e.getMessage());
//        }
//    }
// HATEOAS:
//    TODO: there is something wrong with the entity model, so it can now pulish it, but the rest will give error
    @PostMapping("/hotels")
    public ResponseEntity<?> createHotelBooking(@RequestBody String bookingDetails) {
        try {
            String messageId = publisherService.publishMessage("hotel-booking-requests", bookingDetails);
            EntityModel<String> resource = bookingToEntityModel("hotel", messageId, bookingDetails);
            return ResponseEntity.created(WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(BookingController.class)
                    .createHotelBooking(bookingDetails)).toUri()).body(resource);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error processing hotel booking: " + e.getMessage());
        }
    }

    // Post a new flight booking
//    @PostMapping("/flights")
//    public ResponseEntity<?> createFlightBooking(@RequestBody String bookingDetails) {
//        try {
//            String messageId = publisherService.publishMessage("flight-booking-requests", bookingDetails);
//            return ResponseEntity.status(HttpStatus.CREATED).body("Flight booking request submitted successfully. Message ID: " + messageId);
//        } catch (Exception e) {
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error processing flight booking: " + e.getMessage());
//        }
//    }
    @PostMapping("/flights")
    public ResponseEntity<?> createFlightBooking(@RequestBody String bookingDetails) {
        try {
            String messageId = publisherService.publishMessage("flight-booking-requests", bookingDetails);
            EntityModel<String> resource = bookingToEntityModel("flight", messageId, bookingDetails);
            return ResponseEntity.created(WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(BookingController.class)
                    .createFlightBooking(bookingDetails)).toUri()).body(resource);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error processing flight booking: " + e.getMessage());
        }
    }

    @GetMapping("/{type}/{id}")
    public ResponseEntity<?> retrieveBooking(@PathVariable String type, @PathVariable String id) {
        // Simulate fetching booking details, actual implementation needed
        String bookingDetails = "Booking details for " + type + " ID: " + id; // This should be replaced with actual booking data retrieval
        return ResponseEntity.ok(bookingToEntityModel(type, id, bookingDetails));
    }

    @DeleteMapping("/{type}/{id}")
    public ResponseEntity<?> cancelBooking(@PathVariable String type, @PathVariable String id) {
        // Simulate booking cancellation, actual implementation needed
        return ResponseEntity.ok(type.toUpperCase() + " booking cancelled for ID: " + id);
    }

    // Helper method to create EntityModel for a Booking
    private EntityModel<String> bookingToEntityModel(String type, String messageId, String bookingDetails) {
        return EntityModel.of("Booking request submitted successfully. Message ID: " + messageId,
                WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(BookingController.class).retrieveBooking(type, messageId)).withSelfRel(),
                WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(BookingController.class).cancelBooking(type, messageId)).withRel("cancel-booking")
        );
    }
}
