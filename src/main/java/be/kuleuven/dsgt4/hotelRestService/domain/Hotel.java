package be.kuleuven.dsgt4.hotelRestService.domain;
import java.util.Objects;
public class Hotel {

    private static long idCounter = 0;

    private Long id;
    private String name;
    private String location;
    private int availableRooms;

    public Hotel() {}

    public Hotel(String name, String location, int availableRooms) {
        this.id = idCounter++;
        this.name = name;
        this.location = location;
        this.availableRooms = availableRooms;
    }

    // Getters and setters
    // Getters
    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getLocation() {
        return location;
    }

    public int getAvailableRooms() {
        return availableRooms;
    }

    // Setters
    public void setId(Long id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public void setAvailableRooms(int availableRooms) {
        this.availableRooms = availableRooms;
    }

    // Equals and hashCode methods
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Hotel hotel = (Hotel) o;
        return availableRooms == hotel.availableRooms &&
                Objects.equals(id, hotel.id) &&
                Objects.equals(name, hotel.name) &&
                Objects.equals(location, hotel.location);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, location, availableRooms);
    }
}

