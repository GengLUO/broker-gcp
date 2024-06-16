package be.kuleuven.dsgt4.hotelRestService.domain;

import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import javax.annotation.PostConstruct;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class HotelRepository {

    // map: id -> hotel
    private static final Map<Long, Hotel> hotels = new ConcurrentHashMap<>();

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

    public boolean updateHotelBooking(Long hotelId, int newRooms) {
        Hotel hotel = hotels.get(hotelId);
        if (hotel != null) {
            hotel.setAvailableRooms(hotel.getAvailableRooms() + newRooms);
            return true;
        }
        return false;
    }

    // Create
    public synchronized void addHotel(Hotel hotel) {
        hotels.put(hotel.getId(), hotel);
    }

    // Update
    public synchronized boolean updateHotel(Long id, Hotel updatedHotel) {
        if (hotels.containsKey(id)) {
            hotels.put(id, updatedHotel);
            return true;
        }
        return false;
    }

    // Delete
    public synchronized boolean deleteHotel(Long id) {
        if (hotels.containsKey(id)) {
            hotels.remove(id);
            return true;
        }
        return false;
    }

    //PREPARE
    public synchronized boolean prepareHotel(Long id, int nrOfChangedSeat) {
        Hotel hotel = hotels.get(id);
        if (hotel != null && hotel.getAvailableRooms() >= nrOfChangedSeat) {
            hotel.setAvailableRooms(hotel.getAvailableRooms() - nrOfChangedSeat);
            System.out.println("Prepare hotel!");
            return true;
        }
        return false;
    }

    //COMMIT
    public synchronized boolean commitHotel(Long id) {
        return hotels.containsKey(id);
    }

    //ROLLBACK
    public synchronized boolean rollbackHotel(Long id, int nrOfChangedSeat) {
        Hotel hotel = hotels.get(id);
        if (hotel != null) {
            hotel.setAvailableRooms(hotel.getAvailableRooms() + nrOfChangedSeat);
            return true;
        }
        return false;
    }
}
