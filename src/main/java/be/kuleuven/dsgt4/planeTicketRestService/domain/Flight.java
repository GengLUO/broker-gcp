package be.kuleuven.dsgt4.planeTicketRestService.domain;

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
    // Getter for id
    public Long getId() {
        return id;
    }

    // Setter for id
    public void setId(Long id) {
        this.id = id;
    }

    // Getter for origin
    public String getOrigin() {
        return origin;
    }

    // Setter for origin
    public void setOrigin(String origin) {
        this.origin = origin;
    }

    // Getter for destination
    public String getDestination() {
        return destination;
    }

    // Setter for destination
    public void setDestination(String destination) {
        this.destination = destination;
    }

    // Getter for availableSeats
    public int getAvailableSeats() {
        return availableSeats;
    }

    // Setter for availableSeats
    public void setAvailableSeats(int availableSeats) {
        this.availableSeats = availableSeats;
    }
}
