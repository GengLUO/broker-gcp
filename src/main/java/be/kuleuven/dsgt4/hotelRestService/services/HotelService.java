package be.kuleuven.dsgt4.hotelRestService.services;

import be.kuleuven.dsgt4.hotelRestService.domain.Hotel;
import be.kuleuven.dsgt4.hotelRestService.domain.HotelRepository;
import be.kuleuven.dsgt4.hotelRestService.exceptions.HotelNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Map;

@Service
public class HotelService {

    @Autowired
    private HotelRepository hotelRepository;

    public Collection<Hotel> getAllHotels() {
        return hotelRepository.getAllHotels();
    }

    public Hotel getHotelById(Long id) {
        return hotelRepository.getHotelById(id)
                .orElseThrow(() -> new HotelNotFoundException(id));
    }

    public boolean bookHotel(Long hotelId, int rooms) {
        return hotelRepository.bookHotel(hotelId, rooms);
    }

    public boolean isHotelAvailable(Long hotelId, int rooms) {
        return hotelRepository.isHotelAvailable(hotelId, rooms);
    }

    public boolean cancelHotel(Long hotelId, int rooms) {
        return hotelRepository.cancelHotel(hotelId, rooms);
    }

    public void processBookingRequest(Map<String, Object> message) {
        Long hotelId = ((Number) message.get("hotelId")).longValue();
        int rooms = (int) message.get("rooms");
        boolean success = bookHotel(hotelId, rooms);
        if (!success) {
            throw new RuntimeException("Booking failed for hotel ID: " + hotelId);
        }
    }
}
