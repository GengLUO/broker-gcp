package be.kuleuven.dsgt4.hotelRestService.controllers;

import be.kuleuven.dsgt4.hotelRestService.domain.Hotel;
import be.kuleuven.dsgt4.hotelRestService.domain.HotelRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.google.gson.Gson;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/hotels")
public class HotelRestController {

    @Autowired
    private HotelRepository hotelRepository;

    private static final String API_KEY = "Iw8zeveVyaPNWonPNaU0213uw3g6Ei";
    private final Gson gson = new Gson();

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

    @PostMapping("/book")
    public ResponseEntity<String> bookHotel(@RequestBody Map<String, Object> bookingDetails, @RequestParam String key) {
        if (!API_KEY.equals(key)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        Long hotelId = Long.valueOf(bookingDetails.get("hotelId").toString());
        int rooms = (int) bookingDetails.get("roomsBooked");
        boolean success = hotelRepository.bookHotel(hotelId, rooms);
        return success ? ResponseEntity.ok("Hotel booked") : ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Booking failed");
    }

    @GetMapping("/available")
    public ResponseEntity<Boolean> isHotelAvailable(@RequestParam Long hotelId, @RequestParam int rooms, @RequestParam String key) {
        if (!API_KEY.equals(key)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        boolean available = hotelRepository.isHotelAvailable(hotelId, rooms);
        return ResponseEntity.ok(available);
    }

    @PostMapping("/cancel")
    public ResponseEntity<String> cancelHotel(@RequestParam Long hotelId, @RequestParam int rooms, @RequestParam String key) {
        if (!API_KEY.equals(key)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        boolean success = hotelRepository.cancelHotel(hotelId, rooms);
        return success ? ResponseEntity.ok("Hotel booking cancelled") : ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Cancellation failed");
    }

    @PostMapping("/pubsub/push")
    public ResponseEntity<String> handlePubSubPush(@RequestBody Map<String, Object> message) {
        String packageId = (String) message.get("packageId");
        String userId = (String) message.get("userId");
        Long hotelId = Long.valueOf(message.get("hotelId").toString());
        int rooms = (int) message.get("roomsBooked");

        boolean success = hotelRepository.bookHotel(hotelId, rooms);

        // Respond with booking result
        Map<String, Object> response = new HashMap<>();
        response.put("packageId", packageId);
        response.put("userId", userId);
        response.put("hotelId", hotelId);
        response.put("roomsBooked", rooms);
        response.put("success", success);

        // Here you would normally publish the response message to a Pub/Sub topic
        // For simplicity, we'll just print it
        System.out.println("Hotel booking response: " + response);

        return ResponseEntity.ok("Message processed");
    }
}
