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
        String messageType = (String) message.get("type");
        switch (messageType) {
            case "hotel-add-requests":
                return handleHotelAddRequest(message);
            case "hotel-cancel-requests":
                return handleHotelCancelRequest(message);
            case "hotel-update-requests":
                return handleHotelUpdateRequest(message);
            default:
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid message type");
        }
    }

    private ResponseEntity<String> handleHotelAddRequest(Map<String, Object> message) {
        Long hotelId = Long.valueOf(message.get("hotelId").toString());
        int rooms = (int) message.get("roomsBooked");
        boolean success = hotelRepository.bookHotel(hotelId, rooms);

        return success ? ResponseEntity.ok("Hotel booked") : ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Booking failed");
    }

    private ResponseEntity<String> handleHotelCancelRequest(Map<String, Object> message) {
        Long hotelId = Long.valueOf(message.get("hotelId").toString());
        int rooms = (int) message.get("roomsBooked");
        boolean success = hotelRepository.cancelHotel(hotelId, rooms);

        return success ? ResponseEntity.ok("Hotel booking cancelled") : ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Cancellation failed");
    }

    private ResponseEntity<String> handleHotelUpdateRequest(Map<String, Object> message) {
        Long hotelId = Long.valueOf(message.get("hotelId").toString());
        int newRooms = (int) message.get("newRoomsBooked");
        boolean success = hotelRepository.updateHotelBooking(hotelId, newRooms);

        return success ? ResponseEntity.ok("Hotel booking updated") : ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Update failed");
    }
}
