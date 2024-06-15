package be.kuleuven.dsgt4.flightRestService.controllers;

import be.kuleuven.dsgt4.flightRestService.exceptions.FlightNotFoundException;
import be.kuleuven.dsgt4.flightRestService.services.TransactionService;
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

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.Optional;

@RestController
@RequestMapping("/flights")
public class FlightRestController {

    @Autowired
    private FlightRepository flightRepository;
    @Autowired
    private TransactionService transactionService;
    private static final String API_KEY = "Iw8zeveVyaPNWonPNaU0213uw3g6Ei";

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

    //RESERVE
    @PostMapping("/pubsub/push")
    public ResponseEntity<String> receiveMessage(@RequestBody Map<String, Object> messageWrapper) {
        try {
            Map<String, Object> message = (Map<String, Object>) messageWrapper.get("message");
            String base64EncodedData = (String) message.get("data");
            String decodedData = new String(Base64.getDecoder().decode(base64EncodedData));
            System.out.println("Decoded message data: " + decodedData);

            // Parse the decoded data
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> dataMap = mapper.readValue(decodedData, Map.class);

            // Extract required fields
            String packageId = (String) dataMap.get("packageId");
            Long flightId = Long.parseLong((String) dataMap.get("flightId"));
            int seats = Integer.parseInt((String) dataMap.get("seatsBooked"));
            System.out.println("flightId = " + flightId + "seats = " + seats);

            // Call flightRepository to prepare flight
            boolean success = flightRepository.prepareFlight(flightId, seats);

            if (success) {
                System.out.println("Successfully booked flight for packageId: " + packageId);
                transactionService.confirmAction(packageId, flightId);

                return ResponseEntity.ok("Flight booked successfully");
            } else {
                System.out.println("Booking failed for packageId: " + packageId);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Booking failed");
            }
        } catch (ClassCastException | IllegalArgumentException | NullPointerException |
                 IOException e) {
            // Handle exceptions gracefully
            System.err.println("Error processing message: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error processing message");
        }
    }

    // Commit a transaction
    @PostMapping("/commit/{id}")
    public ResponseEntity<EntityModel<Flight>> commitFlight(@PathVariable Long id, @PathVariable int seats) {
        boolean exists = flightRepository.commitFlight(id);
        if (exists) {
            Flight flight = flightRepository.getFlightById(id).orElseThrow(() -> new FlightNotFoundException(id));
            EntityModel<Flight> entityModel = EntityModel.of(flight);
            entityModel.add(WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(FlightRestController.class).commitFlight(id, seats)).withSelfRel());
            entityModel.add(WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(FlightRestController.class).rollbackFlight(id, seats)).withRel("rollback"));
            return ResponseEntity.ok(entityModel);
        }
        return ResponseEntity.notFound().build();
    }

    // Rollback a transaction
    @PostMapping("/rollback/{id}/{seats}")
    public ResponseEntity<EntityModel<Flight>> rollbackFlight(@PathVariable Long id, @PathVariable int seats) {
        boolean success = flightRepository.rollbackFlight(id, seats);
        if (success) {
            Flight flight = flightRepository.getFlightById(id).orElseThrow(() -> new FlightNotFoundException(id));
            EntityModel<Flight> entityModel = EntityModel.of(flight);
            entityModel.add(WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(FlightRestController.class).rollbackFlight(id, seats)).withSelfRel());
            entityModel.add(WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(FlightRestController.class).commitFlight(id, seats)).withRel("commit"));
            return ResponseEntity.ok(entityModel);
        }
        return ResponseEntity.notFound().build();
    }

}
