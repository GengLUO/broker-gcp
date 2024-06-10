package be.kuleuven.dsgt4.controller;

import be.kuleuven.dsgt4.broker.BrokerPublisherService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
//TODO:
// HATEOAS,
// Integration with Authentication,
// Error Handling,
// Data validation
public class BookingController {

    private final BrokerPublisherService publisherService;

    @Autowired
    public BookingController(BrokerPublisherService publisherService) {
        this.publisherService = publisherService;
    }

    // Post a new hotel booking
    @PostMapping("/bookings/hotels")
    public ResponseEntity<?> createHotelBooking(@RequestBody String bookingDetails) {
        try {
            String messageId = publisherService.publishMessage("hotel-booking-requests", bookingDetails);
            return ResponseEntity.status(HttpStatus.CREATED).body("Hotel booking request submitted successfully. Message ID: " + messageId);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error processing hotel booking: " + e.getMessage());
        }
    }

    // Post a new flight booking
    @PostMapping("/bookings/flights")
    public ResponseEntity<?> createFlightBooking(@RequestBody String bookingDetails) {
        try {
            String messageId = publisherService.publishMessage("flight-booking-requests", bookingDetails);
            return ResponseEntity.status(HttpStatus.CREATED).body("Flight booking request submitted successfully. Message ID: " + messageId);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error processing flight booking: " + e.getMessage());
        }
    }
}
