package be.kuleuven.dsgt4.broker.controller;

import be.kuleuven.dsgt4.broker.services.BrokerPublisherService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import be.kuleuven.dsgt4.broker.domain.TravelPackage;
import be.kuleuven.dsgt4.broker.domain.ItemType;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

//TODO:
// HATEOAS -> use WebMvcLinkBuilder. (not CollectionModel) -> DONE
// Integration with Authentication -> is it needed? is so, check firestore controller for usage example
// Error Handling,
// Data validation

@RestController
@RequestMapping("/bookings")
public class BrokerController {

    private final BrokerPublisherService publisherService;

    @Autowired
    public BrokerController(BrokerPublisherService publisherService) {
        this.publisherService = publisherService;
    }

    @PostMapping("/{userId}")
    public ResponseEntity<?> createTravelPackage(@PathVariable String userId) {
        TravelPackage travelPackage = publisherService.createTravelPackage(userId);
        EntityModel<String> resource = bookingToEntityModel(userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(resource);
    }

    @PostMapping("/book/{userId}")
    public ResponseEntity<?> bookTravelPackage(@PathVariable String userId) {
        boolean success = publisherService.bookTravelPackage(userId);
        if (success) {
            return ResponseEntity.ok("Travel package booked successfully for user ID: " + userId);
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to book travel package for user ID: " + userId);
        }
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

    @DeleteMapping("/{userId}")
    public ResponseEntity<?> clearTravelPackage(@PathVariable String userId) {
        boolean success = publisherService.clearTravelPackage(userId);
        if (success) {
            return ResponseEntity.ok("Travel package cancelled for user ID: " + userId);
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to cancel travel package for user ID: " + userId);
        }
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


    //这段代码的作用是创建一个包含HATEOAS链接的 EntityModel 实例
    private EntityModel<String> bookingToEntityModel(String userId) {
        return EntityModel.of("Booking request submitted successfully. User ID: " + userId,
                WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(BrokerController.class).getTravelPackage(userId)).withSelfRel(),
                WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(BrokerController.class).clearTravelPackage(userId)).withRel("clear-package")
        );
    }
}
