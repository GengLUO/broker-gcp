package be.kuleuven.dsgt4.hotelRestService;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.env.AbstractEnvironment;
import org.springframework.context.annotation.PropertySource;
@SpringBootApplication
@PropertySource("classpath:hotel-application.properties")
public class HotelRestServiceApplication {
    public static void main(String[] args) {
        //System.setProperty(AbstractEnvironment.DEFAULT_PROFILES_PROPERTY_NAME, "hotel");
        SpringApplication.run(HotelRestServiceApplication.class, args);
    }
}