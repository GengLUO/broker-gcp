package be.kuleuven.dsgt4.flightRestService.domain;

import java.util.Objects;


public class Flight {

    private static long idCounter = 0;

    private Long id;
    private String origin;
    private String destination;
    private int availableSeats;

    public Flight() {}

    public Flight(String origin, String destination, int availableSeats) {
        this.id = idCounter++;
        this.origin = origin;
        this.destination = destination;
        this.availableSeats = availableSeats;
    }

    // Getters and setters

    // Getters
    public Long getId() { return id;}

    public String getOrigin() { return origin;}

    public String getDestination() { return destination;}

    public int getAvailableSeats() { return availableSeats;}

    // Setters
    public void setId(Long id) { this.id = id; }

    public void setOrigin(String origin) { this.origin = origin;}

    public void setDestination(String destination) { this.destination = destination;}

    public void setAvailableSeats(int availableSeats) { this.availableSeats = availableSeats;}

    // Equals and hashCode methods
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Flight flight = (Flight) o;
        return availableSeats == flight.availableSeats &&
                Objects.equals(id, flight.id) &&
                Objects.equals(origin, flight.origin) &&
                Objects.equals(destination, flight.destination);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, origin, destination, availableSeats);
    }
}