package be.kuleuven.dsgt4.broker.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/feedback")
public class SupplierFeedbackController {

    private final BrokerRestController brokerRestController;

    @Autowired
    public SupplierFeedbackController(BrokerRestController brokerRestController) {
        this.brokerRestController = brokerRestController;
    }
    @PostMapping("/api/confirmHotel")
    @ResponseBody
    public ResponseEntity<?> confirmHotel(@RequestBody String packageId) {
        return brokerRestController.confirmHotelBooking(packageId);
    }

    @PostMapping("/api/confirmFlight")
    @ResponseBody
    public ResponseEntity<?> confirmFlight(@RequestBody String packageId) {
        return brokerRestController.confirmFlightBooking(packageId);

    }
    
}
