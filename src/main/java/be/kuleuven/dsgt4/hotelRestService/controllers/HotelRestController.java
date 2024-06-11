package be.kuleuven.dsgt4.hotelRestService.controllers;


import be.kuleuven.dsgt4.hotelRestService.domain.Hotel;
import be.kuleuven.dsgt4.hotelRestService.services.HotelService;
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
@RequestMapping("/hotels")
public class HotelRestController {

    @Autowired
    private HotelService hotelService;

    private static final String API_KEY = "Iw8zeveVyaPNWonPNaU0213uw3g6Ei";

    @GetMapping("/all")
    public ResponseEntity<CollectionModel<EntityModel<Hotel>>> getHotels(@RequestParam String key) {
        if (!API_KEY.equals(key)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        List<EntityModel<Hotel>> hotels = hotelService.getAllHotels().stream()
                .map(hotel -> EntityModel.of(hotel,
                        WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(HotelRestController.class).getHotel(hotel.getId(), key)).withSelfRel()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(CollectionModel.of(hotels, WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(HotelRestController.class).getHotels(key)).withSelfRel()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<EntityModel<Hotel>> getHotel(@PathVariable Long id, @RequestParam String key) {
        if (!API_KEY.equals(key)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        Hotel hotel = hotelService.getHotelById(id);
        return ResponseEntity.ok(EntityModel.of(hotel, WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(HotelRestController.class).getHotel(id, key)).withSelfRel()));
    }

    @PostMapping("/book")
    public ResponseEntity<String> bookHotel(@RequestParam Long hotelId, @RequestParam int rooms, @RequestParam String key) {
        if (!API_KEY.equals(key)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        boolean success = hotelService.bookHotel(hotelId, rooms);
        return success ? ResponseEntity.ok("Hotel booked") : ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Booking failed");
    }

    @GetMapping("/available")
    public ResponseEntity<Boolean> isHotelAvailable(@RequestParam Long hotelId, @RequestParam int rooms, @RequestParam String key) {
        if (!API_KEY.equals(key)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        boolean available = hotelService.isHotelAvailable(hotelId, rooms);
        return ResponseEntity.ok(available);
    }

    @PostMapping("/cancel")
    public ResponseEntity<String> cancelHotel(@RequestParam Long hotelId, @RequestParam int rooms, @RequestParam String key) {
        if (!API_KEY.equals(key)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        boolean success = hotelService.cancelHotel(hotelId, rooms);
        return success ? ResponseEntity.ok("Hotel booking cancelled") : ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Cancellation failed");
    }
}
