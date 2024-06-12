package be.kuleuven.dsgt4.hotelRestService.domain;
//import org.springframework.data.jpa.repository.JpaRepository;


import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import javax.annotation.PostConstruct;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Component
public class HotelRepository {

    // map: id -> hotel
    private static final Map<Long, Hotel> hotels = new HashMap<>();

    @PostConstruct
    public void initData() {
        Hotel hotel1 = new Hotel("Grand Hotel", "New York", 50);
        Hotel hotel2 = new Hotel("Ocean View", "Miami", 30);
        Hotel hotel3 = new Hotel("Mountain Retreat", "Denver", 20);

        hotels.put(hotel1.getId(), hotel1);
        hotels.put(hotel2.getId(), hotel2);
        hotels.put(hotel3.getId(), hotel3);
    }

    public Collection<Hotel> getAllHotels() {
        return hotels.values();
    }

    public Optional<Hotel> getHotelById(Long id) {
        Assert.notNull(id, "The hotel id must not be null");
        return Optional.ofNullable(hotels.get(id));
    }

    public boolean bookHotel(Long hotelId, int rooms) {
        Hotel hotel = hotels.get(hotelId);
        if (hotel != null && hotel.getAvailableRooms() >= rooms) {
            hotel.setAvailableRooms(hotel.getAvailableRooms() - rooms);
            return true;
        }
        return false;
    }

    public boolean isHotelAvailable(Long hotelId, int rooms) {
        Hotel hotel = hotels.get(hotelId);
        return hotel != null && hotel.getAvailableRooms() >= rooms;
    }

    public boolean cancelHotel(Long hotelId, int rooms) {
        Hotel hotel = hotels.get(hotelId);
        if (hotel != null) {
            hotel.setAvailableRooms(hotel.getAvailableRooms() + rooms);
            return true;
        }
        return false;
    }
    // You can add more methods as needed, such as adding, updating, or deleting hotels.
}