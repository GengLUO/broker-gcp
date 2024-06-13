package be.kuleuven.dsgt4.flightRestService;

import be.kuleuven.dsgt4.flightRestService.services.FlightService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.PropertySource;

@SpringBootApplication
@PropertySource("classpath:plane-application.properties")
public class FlightRestServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(FlightRestServiceApplication.class, args);

        FlightService flightService = new FlightService();
        try {
            flightService.startSubscriber();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

// package be.kuleuven.dsgt4.flightRestService;

// import org.springframework.boot.SpringApplication;
// import org.springframework.boot.autoconfigure.SpringBootApplication;
// import org.springframework.context.annotation.PropertySource;

// @SpringBootApplication
// @PropertySource("classpath:plane-application.properties")
// public class FlightRestServiceApplication {
//     public static void main(String[] args) {
//         SpringApplication.run(FlightRestServiceApplication.class, args);
//     }
// }