package be.kuleuven.dsgt4.broker.controllers;

import be.kuleuven.dsgt4.broker.domain.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import be.kuleuven.dsgt4.auth.WebSecurityConfig;

import java.util.Map;
import java.util.UUID;

@Controller
@RequestMapping("/travel")
public class UIDataController {

    private final BrokerRestController brokerRestController;

    @Autowired
    public UIDataController(BrokerRestController brokerRestController) {
        this.brokerRestController = brokerRestController;
    }

    @PostMapping("/createPackage")
    @ResponseBody
    public ResponseEntity<?> createTravelPackage(@RequestBody Map<String, Object> packageDetails) {
        // get the userId from packageDetails
        String userId = (String) packageDetails.get("userId");
        ResponseEntity<?> responseEntity = brokerRestController.createTravelPackage(packageDetails);

        if (responseEntity.getStatusCode() == HttpStatus.CREATED) {
            EntityModel<Map<String, String>> entityModel = (EntityModel<Map<String, String>>) responseEntity.getBody();
            Map<String, String> content = entityModel.getContent();
            String packageId = content.get("packageId");
            System.out.println("Created Package ID: " + packageId + " for user: " + userId);
            return ResponseEntity.ok(Map.of("packageId", packageId));
        } else {
            return responseEntity;
        }
    }

    @PostMapping("/addFlight")
    @ResponseBody
    public ResponseEntity<?> addFlightToTravelPackage(@RequestBody Map<String, Object> flightDetails) {
        // get the userId from flightDetails
        String userId = (String) flightDetails.get("userId");
        String packageId = (String) flightDetails.get("packageId");
        System.out.println("Package ID passed to addFlightToTravelPackage method: " + packageId);  // Print out packageId
        System.out.println("Flight details: " + flightDetails);
        return brokerRestController.addFlightToTravelPackage(userId, packageId, flightDetails);
    }

    @PostMapping("/addHotel")
    @ResponseBody
    public ResponseEntity<?> addHotelToTravelPackage(@RequestBody Map<String, Object> hotelDetails) {
        String userId = (String) hotelDetails.get("userId");
        String packageId = (String) hotelDetails.get("packageId");
        System.out.println("Package ID passed to addHotelToTravelPackage method: " + packageId);  // Print out packageId
        System.out.println("Hotel details: " + hotelDetails);
       return brokerRestController.addHotelToTravelPackage(userId, packageId, hotelDetails);
    }

    @PostMapping("/bookPackage")
    @ResponseBody
    public ResponseEntity<?> bookTravelPackage(@RequestBody Map<String, Object> bookingDetails) {
        // get the userId from bookingDetails
        String userId = (String) bookingDetails.get("userId");
        // 打印 bookingDetails 的所有内容
        System.out.println("Booking Details passed to bookTravelPackage method:");
        for (Map.Entry<String, Object> entry : bookingDetails.entrySet()) {
            System.out.println(entry.getKey() + ": " + entry.getValue());
        }

        String packageId = (String) bookingDetails.get("packageId");
        return brokerRestController.bookTravelPackage(userId, packageId, bookingDetails);
    }

}
