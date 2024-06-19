package be.kuleuven.dsgt4.broker.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TravelPackage {
    private List<String> flights;
    private List<String> hotels;
    private String flightDestination;
    private String hotelDestination;
    private String flightDate;
    private String hotelDate;
    private int hotelDays;
    private int seatsBooked;
    private int roomsBooked;
    private String customerName;
    private String userId;
    private String packageId;
    private boolean flightConfirmStatus;
    private boolean hotelConfirmStatus;

public TravelPackage(Map<String, Object> bookingDetails) {
    try {
        this.userId = (String) bookingDetails.get("userId");
        this.packageId = (String) bookingDetails.get("packageId");
        this.flights = ((List<?>) bookingDetails.get("flights")).stream().map(Object::toString).collect(Collectors.toList());
        this.hotels = ((List<?>) bookingDetails.get("hotels")).stream().map(Object::toString).collect(Collectors.toList());
        this.flightDestination = (String) bookingDetails.get("flightDestination");
        this.hotelDestination = (String) bookingDetails.get("hotelDestination");
        this.flightDate = (String) bookingDetails.get("flightDate");
        this.hotelDate = (String) bookingDetails.get("hotelDate");
        this.hotelDays = ((Number) bookingDetails.get("hotelDays")).intValue();
        this.seatsBooked = ((Number) bookingDetails.get("seatsBooked")).intValue();
        this.roomsBooked = ((Number) bookingDetails.get("roomsBooked")).intValue();
        this.customerName = (String) bookingDetails.get("customerName");
        this.flightConfirmStatus = false;
        this.hotelConfirmStatus = false;

        // print the travel package flights and hotels
        System.out.println("Travel Package Flights: " + flights);
        System.out.println("Travel Package Hotels: " + hotels);
    } catch (ClassCastException e) {
        throw new IllegalArgumentException("Invalid data type in booking details", e);
    }
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

    public String getFlightDestination() {
        return flightDestination;
    }

    public String getHotelDestination() {
        return hotelDestination;
    }

    public String getFlightDate() {
        return flightDate;
    }

    public String getHotelDate() {
        return hotelDate;
    }

    public int getHotelDays() {
        return hotelDays;
    }

    public int getSeatsBooked() {
        return seatsBooked;
    }

    public int getRoomsBooked() {
        return roomsBooked;
    }

    public String getCustomerName() {
        return customerName;
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

    public boolean isFlightConfirmStatus() {
        return flightConfirmStatus;
    }

    public void setFlightConfirmStatus(boolean flightConfirmStatus) {
        this.flightConfirmStatus = flightConfirmStatus;
    }

    public boolean isHotelConfirmStatus() {
        return hotelConfirmStatus;
    }

    public void setHotelConfirmStatus(boolean hotelConfirmStatus) {
        this.hotelConfirmStatus = hotelConfirmStatus;
    }

    public void clear() {
        flights.clear();
        hotels.clear();
    }
}
