package be.kuleuven.dsgt4.planeTicketRestService.controllers;


import be.kuleuven.dsgt4.planeTicketRestService.domain.Flight;
import be.kuleuven.dsgt4.planeTicketRestService.services.FlightService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/flights")
public class FlightRestController {

    @Autowired
    private FlightService flightService;

    private static final String API_KEY = "Iw8zeveVyaPNWonPNaU0213uw3g6Ei";

    @GetMapping("/all")
    public ResponseEntity<CollectionModel<EntityModel<Flight>>> getFlights(@RequestParam String key) {
        if (!API_KEY.equals(key)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        List<EntityModel<Flight>> flights = flightService.getAllFlights().stream()
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
        Flight flight = flightService.getFlightById(id);
        return ResponseEntity.ok(EntityModel.of(flight, WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(FlightRestController.class).getFlight(id, key)).withSelfRel()));
    }

    @PostMapping("/book")
    public ResponseEntity<String> bookFlight(@RequestParam Long flightId, @RequestParam int seats, @RequestParam String key) {
        if (!API_KEY.equals(key)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        boolean success = flightService.bookFlight(flightId, seats);
        return success ? ResponseEntity.ok("Flight booked") : ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Booking failed");
    }
}