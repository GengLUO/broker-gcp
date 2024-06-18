package be.kuleuven.dsgt4.flightRestService.domain;

import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import javax.annotation.PostConstruct;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class FlightRepository {

    // Map: id -> flight
    private static final Map<Long, Flight> flights = new ConcurrentHashMap<>();

    @PostConstruct
    public void initData() {
        Flight flight1 = new Flight("New York", "London", 200);
        Flight flight2 = new Flight("Paris", "Tokyo", 150);
        Flight flight3 = new Flight("Sydney", "Los Angeles", 100);

        flights.put(flight1.getId(), flight1);
        flights.put(flight2.getId(), flight2);
        flights.put(flight3.getId(), flight3);
    }

    public Collection<Flight> getAllFlights() {
        return flights.values();
    }

    public Optional<Flight> getFlightById(Long id) {
        Assert.notNull(id, "The flight id must not be null");
        return Optional.ofNullable(flights.get(id));
    }

    public boolean bookFlight(Long flightId, int seats) {
        Flight flight = flights.get(flightId);
        if (flight != null && flight.getAvailableSeats() >= seats) {
            flight.setAvailableSeats(flight.getAvailableSeats() - seats);
            return true;
        }
        return false;
    }

    public boolean isFlightAvailable(Long flightId, int seats) {
        Flight flight = flights.get(flightId);
        return flight != null && flight.getAvailableSeats() >= seats;
    }

    public boolean cancelFlight(Long flightId, int seats) {
        Flight flight = flights.get(flightId);
        if (flight != null) {
            flight.setAvailableSeats(flight.getAvailableSeats() + seats);
            return true;
        }
        return false;
    }

    public boolean updateFlightBooking(Long flightId, int newSeats) {
        Flight flight = flights.get(flightId);
        if (flight != null) {
            flight.setAvailableSeats(flight.getAvailableSeats() + newSeats);
            return true;
        }
        return false;
    }

    // Create
    public synchronized void addFlight(Flight flight) {
        flights.put(flight.getId(), flight);
    }

    // Update
    public synchronized boolean updateFlight(Long id, Flight updatedFlight) {
        if (flights.containsKey(id)) {
            flights.put(id, updatedFlight);
            return true;
        }
        return false;
    }

    // Delete
    public synchronized boolean deleteFlight(Long id) {
        if (flights.containsKey(id)) {
            flights.remove(id);
            return true;
        }
        return false;
    }

    //PREPARE
    public synchronized boolean prepareFlight(Long id, int nrOfChangedSeat) {
        Flight flight = flights.get(id);
        if (flight != null && flight.getAvailableSeats() >= nrOfChangedSeat) {
            flight.setAvailableSeats(flight.getAvailableSeats() - nrOfChangedSeat);
            System.out.println("Prepare flight!");
            return true;
        }
        return false;
    }

    //COMMIT
    public synchronized boolean commitFlight(Long id) {
        return flights.containsKey(id);
    }

    //ROLLBACK
    public synchronized boolean rollbackFlight(Long id, int nrOfChangedSeat) {
        Flight flight = flights.get(id);
        if (flight != null) {
            flight.setAvailableSeats(flight.getAvailableSeats() + nrOfChangedSeat);
            return true;
        }
        return false;
    }
}
