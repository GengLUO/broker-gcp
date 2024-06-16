package be.kuleuven.dsgt4.hotelRestService.controllers;

import be.kuleuven.dsgt4.hotelRestService.domain.Hotel;
import be.kuleuven.dsgt4.hotelRestService.domain.HotelRepository;
import be.kuleuven.dsgt4.hotelRestService.exceptions.HotelNotFoundException;
import be.kuleuven.dsgt4.hotelRestService.services.TransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/hotels")
public class HotelRestController {

    @Autowired
    private HotelRepository hotelRepository;
    @Autowired
    private TransactionService transactionService;
    private static final String API_KEY = "Iw8zeveVyaPNWonPNaU0213uw3g6Ei";


    //Simple test function to test subscriber function
    @PostMapping("/test")
    public ResponseEntity<String> test(@RequestBody String body) {
        System.out.println("Receieved test request");
        System.out.println(body);
        return ResponseEntity.ok("Received");
    }

    @GetMapping("/all")
    public ResponseEntity<CollectionModel<EntityModel<Hotel>>> getHotels(@RequestParam String key) {
        if (!API_KEY.equals(key)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        List<EntityModel<Hotel>> hotels = hotelRepository.getAllHotels().stream()
                .map(hotel -> EntityModel.of(hotel,
                        WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(HotelRestController.class).getHotel(hotel.getId().toString(), key)).withSelfRel()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(CollectionModel.of(hotels, WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(HotelRestController.class).getHotels(key)).withSelfRel()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<EntityModel<Hotel>> getHotel(@PathVariable String id, @RequestParam String key) {
        if (!API_KEY.equals(key)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        Optional<Hotel> optionalHotel = hotelRepository.getHotelById(Long.valueOf(id));
        if (optionalHotel.isPresent()) {
            Hotel hotel = optionalHotel.get();
            return ResponseEntity.ok(EntityModel.of(hotel,
                    WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(HotelRestController.class).getHotel(id, key)).withSelfRel()));
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    //pubsub
    @PostMapping("/pubsub/push")
    public ResponseEntity<String> receiveMessage(@RequestBody Map<String, Object> messageWrapper) {
        try {
            Map<String, Object> message = (Map<String, Object>) messageWrapper.get("message");
            Map<String, String> attributes = (Map<String, String>) message.get("attributes");

            // Extract required fields from attributes
            String packageId = attributes.get("packageId");
            Long hotelId = Long.parseLong(attributes.get("hotelId"));
            int roomsBooked = Integer.parseInt(attributes.get("roomsBooked"));
            Long flightId = Long.parseLong(attributes.get("flightId"));
            int seatsBooked = Integer.parseInt(attributes.get("seatsBooked"));
            String userId = attributes.get("userId");
            String customerName = attributes.get("customerName");
            String action = attributes.get("action");

            // Print all attributes
            System.out.println("Received message attributes:");
            System.out.println("packageId: " + packageId);
            System.out.println("hotelId: " + hotelId);
            System.out.println("roomsBooked: " + roomsBooked);
            System.out.println("flightId: " + flightId);
            System.out.println("seatsBooked: " + seatsBooked);
            System.out.println("userId: " + userId);
            System.out.println("customerName: " + customerName);

            boolean success = false;
            switch (action) {
                //PREPARE
                case "PREPARE":
                    success = hotelRepository.prepareHotel(hotelId, roomsBooked);
                    if (success) {
                        System.out.println("Successfully booked hotel for packageId: " + packageId);
                        transactionService.confirmAction(packageId);
                        return ResponseEntity.ok("Hotel booked successfully");
                    }
                    break;
                    //COMMIT
                case "COMMIT":
                    success = hotelRepository.commitHotel(hotelId);
                    if (success) {
                        System.out.println("Successfully committed the hotel for hotelId: " + hotelId);
                        return ResponseEntity.ok("Hotel committed successfully");
                    }
                    break;
                    //ROLLBACK
                case "ROLLBACK":
                    success = hotelRepository.rollbackHotel(hotelId, roomsBooked);
                    if (success) {
                        System.out.println("Successfully rolled back hotel for packageId: " + packageId + ", hotelId: " + hotelId + ", roomsBooked: " + roomsBooked);
                        return ResponseEntity.ok("Hotel rolled back successfully");
                    }
                    break;
                default:
                    System.out.println("Unknown action: " + action);
                    break;
            }

            System.out.println("Booking failed for packageId: " + packageId + "action = " + action);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Booking failed");

        } catch (ClassCastException | IllegalArgumentException | NullPointerException e) {
            System.err.println("Error processing message: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error processing message");
        }

    }

    // Commit a transaction using WebClient
    @PostMapping("/commit/{id}/{seats}")
    public ResponseEntity<EntityModel<Hotel>> commitHotel(@PathVariable Long id, @PathVariable int rooms) {
        boolean exists = hotelRepository.commitHotel(id);
        if (exists) {
            Hotel hotel = hotelRepository.getHotelById(id).orElseThrow(() -> new HotelNotFoundException(id));
            EntityModel<Hotel> entityModel = EntityModel.of(hotel);
            entityModel.add(WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(HotelRestController.class).commitHotel(id, rooms)).withSelfRel());
            entityModel.add(WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(HotelRestController.class).rollbackHotel(id, rooms)).withRel("rollback"));
            return ResponseEntity.ok(entityModel);
        }
        return ResponseEntity.notFound().build();
    }

    // Rollback a transaction using WebClient
    @PostMapping("/rollback/{id}/{seats}")
    public ResponseEntity<EntityModel<Hotel>> rollbackHotel(@PathVariable Long id, @PathVariable int rooms) {
        boolean success = hotelRepository.rollbackHotel(id, rooms);
        if (success) {
            Hotel hotel = hotelRepository.getHotelById(id).orElseThrow(() -> new HotelNotFoundException(id));
            EntityModel<Hotel> entityModel = EntityModel.of(hotel);
            entityModel.add(WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(HotelRestController.class).rollbackHotel(id, rooms)).withSelfRel());
            entityModel.add(WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(HotelRestController.class).commitHotel(id, rooms)).withRel("commit"));
            return ResponseEntity.ok(entityModel);
        }
        return ResponseEntity.notFound().build();
    }
}
