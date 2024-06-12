package be.kuleuven.dsgt4.broker.services;

import be.kuleuven.dsgt4.broker.domain.TravelPackage;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class BrokerService {
    @Autowired
    private FlightSupplierService flightSupplierService;

    @Autowired
    private HotelSupplierService hotelSupplierService;

    private static final String API_KEY = "Iw8zeveVyaPNWonPNaU0213uw3g6Ei";

    private final List<TravelPackage> travelPackages = new ArrayList<>();

    private final ObjectMapper objectMapper = new ObjectMapper();

    public TravelPackage createTravelPackage(String userId) {
        TravelPackage travelPackage = new TravelPackage(userId);
        travelPackages.add(travelPackage);
        return travelPackage;
    }

    public void addFlightToTravelPackage(TravelPackage travelPackage, String flightJson) {
        travelPackage.addFlight(flightJson);
    }

    public void removeFlightFromTravelPackage(TravelPackage travelPackage, String flightJson) {
        travelPackage.removeFlight(flightJson);
    }

    public void addHotelToTravelPackage(TravelPackage travelPackage, String hotelJson) {
        travelPackage.addHotel(hotelJson);
    }

    public void removeHotelFromTravelPackage(TravelPackage travelPackage, String hotelJson) {
        travelPackage.removeHotel(hotelJson);
    }

    public String getHotels() {
        return hotelSupplierService.getHotels(API_KEY);
    }

    public String getHotel(Long id) {
        return hotelSupplierService.getHotel(id, API_KEY);
    }

    public boolean bookHotel(Long hotelId, int rooms) {
        return hotelSupplierService.bookHotel(hotelId, rooms, API_KEY);
    }

    public String getFlights() {
        return flightSupplierService.getFlights(API_KEY);
    }

    public String getFlight(Long id) {
        return flightSupplierService.getFlight(id, API_KEY);
    }

    public boolean bookFlight(Long flightId, int seats) {
        return flightSupplierService.bookFlight(flightId, seats, API_KEY);
    }

    public TravelPackage getTravelPackage(String userId) {
        for (TravelPackage travelPackage : travelPackages) {
            if (travelPackage.getUserId().equals(userId)) {
                return travelPackage;
            }
        }
        return null;
    }

    public boolean bookTravelPackage(TravelPackage travelPackage) {
        List<String> bookedFlights = new ArrayList<>();
        List<String> bookedHotels = new ArrayList<>();

        try {
            // book all flights
            for (String flightJson : travelPackage.getFlights()) {
                Map<String, Object> flightMap = parseFlightJson(flightJson);
                Long flightId = (Long) flightMap.get("id");
                int availableSeats = (int) flightMap.get("availableSeats");
                boolean booked = flightSupplierService.bookFlight(flightId, availableSeats, API_KEY);
                if (!booked) {
                    throw new Exception("Failed to book flight: " + flightId);
                }
                bookedFlights.add(flightJson);
            }

            // book all hotels
            for (String hotelJson : travelPackage.getHotels()) {
                Map<String, Object> hotelMap = parseHotelJson(hotelJson);
                Long hotelId = (Long) hotelMap.get("id");
                int availableRooms = (int) hotelMap.get("availableRooms");
                boolean booked = hotelSupplierService.bookHotel(hotelId, availableRooms, API_KEY);
                if (!booked) {
                    throw new Exception("Failed to book hotel: " + hotelId);
                }
                bookedHotels.add(hotelJson);
            }

            return true;
        } catch (Exception e) {
            System.err.println(e.getMessage());
            // Cancel booked flights
            for (String flightJson : bookedFlights) {
                Map<String, Object> flightMap = parseFlightJson(flightJson);
                Long flightId = (Long) flightMap.get("id");
                int availableSeats = (int) flightMap.get("availableSeats");
                flightSupplierService.cancelFlight(flightId, availableSeats, API_KEY);
            }
            // Cancel booked hotels
            for (String hotelJson : bookedHotels) {
                Map<String, Object> hotelMap = parseHotelJson(hotelJson);
                Long hotelId = (Long) hotelMap.get("id");
                int availableRooms = (int) hotelMap.get("availableRooms");
                hotelSupplierService.cancelHotel(hotelId, availableRooms, API_KEY);
            }
            return false;
        }
    }

    private Map<String, Object> parseFlightJson(String flightJson) {
        try {
            return objectMapper.readValue(flightJson, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse flight JSON", e);
        }
    }

    private Map<String, Object> parseHotelJson(String hotelJson) {
        try {
            return objectMapper.readValue(hotelJson, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse hotel JSON", e);
        }
    }
}
