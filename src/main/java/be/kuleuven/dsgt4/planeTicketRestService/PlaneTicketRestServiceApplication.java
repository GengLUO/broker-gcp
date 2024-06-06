package be.kuleuven.dsgt4.planeTicketRestService;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.PropertySource;

@SpringBootApplication
@PropertySource("classpath:plane-application.properties")
public class PlaneTicketRestServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(PlaneTicketRestServiceApplication.class, args);
    }
}