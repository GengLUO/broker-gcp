package be.kuleuven.dsgt4.flightRestService.controllers;

import org.springframework.context.event.EventListener;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.google.gson.Gson;

import be.kuleuven.dsgt4.flightRestService.domain.Flight;
import be.kuleuven.dsgt4.flightRestService.domain.FlightEvent;
import be.kuleuven.dsgt4.flightRestService.domain.FlightRepository;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.*;
import java.util.stream.Collectors;
import java.util.Optional;

@RestController
@RequestMapping("/flights")
public class FlightRestController {

    @Autowired
    private FlightRepository flightRepository;

    private static final String API_KEY = "Iw8zeveVyaPNWonPNaU0213uw3g6Ei";
    private final Gson gson = new Gson();

    @Autowired
    private WebClient.Builder webClientBuilder;

    private WebClient webClient;

    @GetMapping("/all")
    public ResponseEntity<CollectionModel<EntityModel<Flight>>> getFlights(@RequestParam String key) {
        if (!API_KEY.equals(key)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        List<EntityModel<Flight>> flights = flightRepository.getAllFlights().stream()
                .map(flight -> EntityModel.of(flight,
                        WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(FlightRestController.class).getFlight(flight.getId().toString(), key)).withSelfRel()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(CollectionModel.of(flights, WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(FlightRestController.class).getFlights(key)).withSelfRel()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<EntityModel<Flight>> getFlight(@PathVariable String id, @RequestParam String key) {
        if (!API_KEY.equals(key)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        Optional<Flight> optionalFlight = flightRepository.getFlightById(Long.valueOf(id));
        if (optionalFlight.isPresent()) {
            Flight flight = optionalFlight.get();
            return ResponseEntity.ok(EntityModel.of(flight,
                    WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(FlightRestController.class).getFlight(id, key)).withSelfRel()));
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @PostMapping("/book")
    public ResponseEntity<String> bookFlight(@RequestBody Map<String, Object> bookingDetails, @RequestParam String key) {
        if (!API_KEY.equals(key)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        Long flightId = Long.valueOf(bookingDetails.get("flightId").toString());
        int seats = (int) bookingDetails.get("seatsBooked");
        boolean success = flightRepository.bookFlight(flightId, seats);
        return success ? ResponseEntity.ok("Flight booked") : ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Booking failed");
    }

    @GetMapping("/available")
    public ResponseEntity<Boolean> isFlightAvailable(@RequestParam Long flightId, @RequestParam int seats, @RequestParam String key) {
        if (!API_KEY.equals(key)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        boolean available = flightRepository.isFlightAvailable(flightId, seats);
        return ResponseEntity.ok(available);
    }

    @PostMapping("/cancel")
    public ResponseEntity<String> cancelFlight(@RequestParam Long flightId, @RequestParam int seats, @RequestParam String key) {
        if (!API_KEY.equals(key)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        boolean success = flightRepository.cancelFlight(flightId, seats);
        return success ? ResponseEntity.ok("Flight booking cancelled") : ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Cancellation failed");
    }

    @EventListener
    public ResponseEntity<String> handleFlightEvent(FlightEvent event) {
        Map<String, Object> message = event.getMessageData();
        String messageType = (String) message.get("type");
        switch (messageType) {
            case "flight-add-requests":
                return handleFlightAddRequest(message);
            case "flight-cancel-requests":
                return handleFlightCancelRequest(message);
            case "flight-update-requests":
                return handleFlightUpdateRequest(message);
            default:
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid message type");
        }
    }

    // @PostMapping("/pubsub/push")
    // public ResponseEntity<String> handlePubSubPush(@RequestBody Map<String, Object> message) {
    //     String messageType = (String) message.get("type");
    //     switch (messageType) {
    //         case "flight-add-requests":
    //             return handleFlightAddRequest(message);
    //         case "flight-cancel-requests":
    //             return handleFlightCancelRequest(message);
    //         case "flight-update-requests":
    //             return handleFlightUpdateRequest(message);
    //         default:
    //             return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid message type");
    //     }
    // }

    @PostMapping("/pubsub/push")
    public ResponseEntity<String> handlePubSubPush(@RequestBody Map<String, Object> message) {
        String messageType = (String) message.get("type");
        switch (messageType) {
            case "flight-booking-requests":
                return handleFlightAddRequest(message);
            case "flight-cancel-requests":
                return handleFlightCancelRequest(message);
            case "flight-upd1ate-requests":
                return handleFlightUpdateRequest(message);
            default:
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid message type");
        }
    }

    //RESERVE
    private ResponseEntity<String> handleFlightAddRequest(Map<String, Object> message) {
        Long flightId = Long.valueOf(message.get("flightId").toString());
        int seats = (int) message.get("seatsBooked");
        boolean success = flightRepository.bookFlight(flightId, seats);

        return success ? ResponseEntity.ok("Flight booked") : ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Booking failed");
    }

    // Commit a transaction
    @PostMapping("/commit/{id}")
    public ResponseEntity<?> commitFlight(@PathVariable Long id) {
        boolean exists = flightRepository.commitFlight(id);
        if (exists) {
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }

    // Rollback a transaction
    @PostMapping("/rollback/{id}/{seats}")
    public ResponseEntity<?> rollbackFlight(@PathVariable Long id, @PathVariable int seats) {
        flightRepository.rollbackFlight(id, seats);
        return ResponseEntity.ok().build();
    }

    private ResponseEntity<String> handleFlightCancelRequest(Map<String, Object> message) {
        Long flightId = Long.valueOf(message.get("flightId").toString());
        int seats = (int) message.get("seatsBooked");
        boolean success = flightRepository.cancelFlight(flightId, seats);

        return success ? ResponseEntity.ok("Flight booking cancelled") : ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Cancellation failed");
    }

    private ResponseEntity<String> handleFlightUpdateRequest(Map<String, Object> message) {
        Long flightId = Long.valueOf(message.get("flightId").toString());
        int newSeats = (int) message.get("newSeatsBooked");
        boolean success = flightRepository.updateFlightBooking(flightId, newSeats);

        return success ? ResponseEntity.ok("Flight booking updated") : ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Update failed");
    }

}
