package be.kuleuven.dsgt4.broker.domain;

import java.util.ArrayList;
import java.util.List;

public class TravelPackage {
    private List<String> flights = new ArrayList<>();
    private List<String> hotels = new ArrayList<>();
    private String userId;
    private String packageId;

    public TravelPackage(String userId, String packageId) {
        this.userId = userId;
        this.packageId = packageId;
    }

    public List<String> getFlights() {
        return flights;
    }

    public void addFlight(String flightJson) {
        flights.add(flightJson);
    }

    public void removeFlight(String flightJson) {
        flights.remove(flightJson);
    }

    public List<String> getHotels() {
        return hotels;
    }

    public void addHotel(String hotelJson) {
        hotels.add(hotelJson);
    }

    public void removeHotel(String hotelJson) {
        hotels.remove(hotelJson);
    }

    public String getUserId() {
        return userId;
    }

    public String getPackageId() {
        return packageId;
    }

    public void clear() {
        flights.clear();
        hotels.clear();
    }
}
