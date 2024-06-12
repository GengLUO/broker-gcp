package be.kuleuven.dsgt4.planeTicketRestService.controllers;


import be.kuleuven.dsgt4.planeTicketRestService.domain.Flight;
import be.kuleuven.dsgt4.planeTicketRestService.domain.FlightRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

import java.util.Optional;

@RestController
@RequestMapping("/flights")
public class FlightRestController {

    @Autowired
    private FlightRepository flightRepository;

    private static final String API_KEY = "Iw8zeveVyaPNWonPNaU0213uw3g6Ei";

    @GetMapping("/all")
    public ResponseEntity<CollectionModel<EntityModel<Flight>>> getFlights(@RequestParam String key) {
        if (!API_KEY.equals(key)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        List<EntityModel<Flight>> flights = flightRepository.getAllFlights().stream()
                .map(flight -> EntityModel.of(flight,
                        WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(FlightRestController.class).getFlight(flight.getId(), key)).withSelfRel()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(CollectionModel.of(flights, WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(FlightRestController.class).getFlights(key)).withSelfRel()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<EntityModel<Flight>> getFlight(@PathVariable Long id, @RequestParam String key) {
        if (!API_KEY.equals(key)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        Optional<Flight> optionalFlight = flightRepository.getFlightById(id);
        if (optionalFlight.isPresent()) {
            Flight flight = optionalFlight.get();
            return ResponseEntity.ok(EntityModel.of(flight,
                    WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(FlightRestController.class).getFlight(id, key)).withSelfRel()));
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @PostMapping("/book")
    public ResponseEntity<String> bookFlight(@RequestParam Long flightId, @RequestParam int seats, @RequestParam String key) {
        if (!API_KEY.equals(key)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
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
}