package be.kuleuven.dsgt4.planeTicketRestService.services;

import be.kuleuven.dsgt4.planeTicketRestService.domain.Flight;
import be.kuleuven.dsgt4.planeTicketRestService.domain.FlightRepository;
import be.kuleuven.dsgt4.planeTicketRestService.exceptions.FlightNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Map;

@Service
public class FlightService {

    @Autowired
    private FlightRepository flightRepository;

    public Collection<Flight> getAllFlights() {
        return flightRepository.getAllFlights();
    }

    public Flight getFlightById(Long id) {
        return flightRepository.getFlightById(id)
                .orElseThrow(() -> new FlightNotFoundException(id));
    }

    public boolean bookFlight(Long flightId, int seats) {
        return flightRepository.bookFlight(flightId, seats);
    }

    public boolean isFlightAvailable(Long flightId, int seats) {
        return flightRepository.isFlightAvailable(flightId, seats);
    }

    public boolean cancelFlight(Long flightId, int seats) {
        return flightRepository.cancelFlight(flightId, seats);
    }

    public void processBookingRequest(Map<String, Object> message) {
        // Extract booking details from the Pub/Sub message
        Long flightId = Long.parseLong(message.get("flightId").toString());
        int seats = Integer.parseInt(message.get("seats").toString());
        
        // Book the flight
        if (isFlightAvailable(flightId, seats)) {
            bookFlight(flightId, seats);
        } else {
            throw new RuntimeException("Flight is not available");
        }
    }
}
