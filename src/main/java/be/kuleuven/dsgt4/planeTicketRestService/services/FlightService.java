package be.kuleuven.dsgt4.planeTicketRestService.services;

import be.kuleuven.dsgt4.planeTicketRestService.domain.Flight;
import be.kuleuven.dsgt4.planeTicketRestService.domain.FlightRepository;
import be.kuleuven.dsgt4.planeTicketRestService.exceptions.FlightNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;

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

    // You can add more methods as needed, such as adding, updating, or deleting flights.
}