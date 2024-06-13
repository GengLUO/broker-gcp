package be.kuleuven.dsgt4.flightRestService;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.PropertySource;

@SpringBootApplication
@PropertySource("classpath:plane-application.properties")
public class FlightRestServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(FlightRestServiceApplication.class, args);
    }
}