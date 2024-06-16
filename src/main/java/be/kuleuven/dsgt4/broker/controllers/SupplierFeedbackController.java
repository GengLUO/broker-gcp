package be.kuleuven.dsgt4.broker.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Controller
@RequestMapping("/feedback")
public class SupplierFeedbackController {
    
    private static final Logger logger = LoggerFactory.getLogger(SupplierFeedbackController.class);

    private final BrokerRestController brokerRestController;

    @Autowired
    public SupplierFeedbackController(BrokerRestController brokerRestController) {
        this.brokerRestController = brokerRestController;
    }
    @PostMapping("/confirmHotel")
    // @ResponseBody
    public ResponseEntity<?> confirmHotel(@RequestBody String packageId) {
        logger.info("passing hotel confirmed info packageId to boroker rest controller: " + packageId);
        brokerRestController.confirmHotelBooking(packageId);
        // set response status to 200 OK and message with Hotel booking confirming with packageId
        return ResponseEntity.ok("From hotel supplier: hotel booking confirmed with packageId: " + packageId);
    }

    @PostMapping("/confirmFlight")
    // @ResponseBody
    public ResponseEntity<?> confirmFlight(@RequestBody String packageId) {
        logger.info("passing flight confirmed info packageId to boroker rest controller: " + packageId);
        brokerRestController.confirmFlightBooking(packageId);
        // set response status to 200 OK and message with Flight booking confirming with packageId
        return ResponseEntity.ok("From flight supplier: flight booking confirmed with packageId: " + packageId);

    }

    @GetMapping("/test")
    public ResponseEntity<?> test() {
        return ResponseEntity.ok("SupplierFeedbackController is working");
    }
    
}
