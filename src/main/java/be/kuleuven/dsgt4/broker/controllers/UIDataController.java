package be.kuleuven.dsgt4.broker.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Controller
@RequestMapping("/api/travel")
public class UIDataController {

    private final BrokerRestController brokerRestController;

    @Autowired
    public UIDataController(BrokerRestController brokerRestController) {
        this.brokerRestController = brokerRestController;
    }

    @PostMapping("/createPackage")
    @ResponseBody
    public ResponseEntity<?> createTravelPackage(@RequestBody Map<String, Object> packageDetails) {
        try {
            System.out.println("Received package details: " + packageDetails);
            String userId = (String) packageDetails.get("userId");
            if (userId == null || userId.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("UserId is required.");
            }
            ResponseEntity<?> responseEntity = brokerRestController.createTravelPackage(packageDetails);

            if (responseEntity.getStatusCode() == HttpStatus.CREATED) {
                EntityModel<Map<String, String>> entityModel = (EntityModel<Map<String, String>>) responseEntity.getBody();
                if (entityModel != null) {
                    Map<String, String> content = entityModel.getContent();
                    String packageId = content.get("packageId");
                    System.out.println("Created Package ID: " + packageId + " for user: " + userId);
                    return ResponseEntity.ok(Map.of("packageId", packageId));
                }
            }
            return responseEntity;
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to create travel package: " + e.getMessage());
        }
    }

    @PostMapping("/addFlight")
    @ResponseBody
    public ResponseEntity<?> addFlightToTravelPackage(@RequestBody Map<String, Object> flightDetails) {
        try {
            String userId = (String) flightDetails.get("userId");
            String packageId = (String) flightDetails.get("packageId");
            if (userId == null || userId.isEmpty() || packageId == null || packageId.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("UserId and PackageId are required.");
            }
            System.out.println("Package ID passed to addFlightToTravelPackage method: " + packageId);
            System.out.println("Flight details: " + flightDetails);
            return brokerRestController.addFlightToTravelPackage(packageId, flightDetails);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to add flight to travel package: " + e.getMessage());
        }
    }

    @PostMapping("/addHotel")
    @ResponseBody
    public ResponseEntity<?> addHotelToTravelPackage(@RequestBody Map<String, Object> hotelDetails) {
        try {
            String userId = (String) hotelDetails.get("userId");
            String packageId = (String) hotelDetails.get("packageId");
            if (userId == null || userId.isEmpty() || packageId == null || packageId.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("UserId and PackageId are required.");
            }
            System.out.println("Package ID passed to addHotelToTravelPackage method: " + packageId);
            System.out.println("Hotel details: " + hotelDetails);
            return brokerRestController.addHotelToTravelPackage(packageId, hotelDetails);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to add hotel to travel package: " + e.getMessage());
        }
    }

    @PostMapping("/bookPackage")
    @ResponseBody
    public ResponseEntity<?> bookTravelPackage(@RequestBody Map<String, Object> bookingDetails) {
        try {
            String userId = (String) bookingDetails.get("userId");
            if (userId == null || userId.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("UserId is required.");
            }
            System.out.println("Booking Details passed to bookTravelPackage method:");
            for (Map.Entry<String, Object> entry : bookingDetails.entrySet()) {
                System.out.println(entry.getKey() + ": " + entry.getValue());
            }
            String packageId = (String) bookingDetails.get("packageId");
            if (packageId == null || packageId.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("PackageId is required.");
            }
            return brokerRestController.bookTravelPackage(packageId, bookingDetails);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to book travel package: " + e.getMessage());
        }
    }
}