package be.kuleuven.dsgt4.broker.controllers;

import be.kuleuven.dsgt4.auth.WebSecurityConfig;
import be.kuleuven.dsgt4.broker.domain.User;
import be.kuleuven.dsgt4.broker.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@RestController
@RequestMapping("/api")
public class UserRestController {

    private final UserService userService;

    @Autowired
    public UserRestController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/hello")
    public String hello() {
        System.out.println("Inside hello");
        return "hello world!";
    }

    @GetMapping("/whoami")
    public User whoami() throws InterruptedException, ExecutionException {
        var user = WebSecurityConfig.getUser();
        // if (!user.isManager()) throw new AuthorizationServiceException("You are not a manager");
        if (!user.isManager()) System.out.println("You are not a manager.");;
        // the user id of this user is different than the uid in firestore
        return user;
    }

    @GetMapping("/getUserBookings")
    @ResponseBody
    public ResponseEntity<?> getUserBookings(@RequestParam String userId) {
        try {
            List<Map<String, Object>> bookingDetails = userService.getUserBookings(userId);
            System.out.println(bookingDetails); // Debugging line to check data
            return ResponseEntity.ok(bookingDetails);
        } catch (ExecutionException | InterruptedException e) {
            return ResponseEntity.status(500).body("Error fetching bookings: " + e.getMessage());
        }
    }
    
    
    /** manager role users privliged methods */

    @GetMapping("/getAllOrders")
    @PreAuthorize("hasAuthority('ROLE_MANAGER')")
    public ResponseEntity<?> getAllOrders() {
        try {
            List<Map<String, Object>> orders = userService.getAllOrders();
            return ResponseEntity.ok(orders);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Failed to get orders: " + e.getMessage());
        }
    }

    @GetMapping("/getAllCustomers")
    @PreAuthorize("hasAuthority('ROLE_MANAGER')")
    public ResponseEntity<?> getAllCustomers() {
        try {
            List<Map<String, Object>> customers = userService.getAllCustomers();
            return ResponseEntity.ok(customers);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Failed to get customers: " + e.getMessage());
        }
    }

    @PostMapping("/{userId}")
    public ResponseEntity<?> createUser(@PathVariable String userId, @RequestBody Map<String, Object> userDetails) {
        try {
            userService.createUser(userId, userDetails);
            return ResponseEntity.ok("User created successfully.");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Failed to create user: " + e.getMessage());
        }
    }

    @GetMapping("/{userId}")
    public ResponseEntity<?> getUser(@PathVariable String userId) {
        try {
            Map<String, Object> user = userService.getUser(userId);
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Failed to get user: " + e.getMessage());
        }
    }

    @PutMapping("/{userId}")
    public ResponseEntity<?> updateUser(@PathVariable String userId, @RequestBody Map<String, Object> userDetails) {
        try {
            userService.updateUser(userId, userDetails);
            return ResponseEntity.ok("User updated successfully.");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Failed to update user: " + e.getMessage());
        }
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<?> deleteUser(@PathVariable String userId) {
        try {
            userService.deleteUser(userId);
            return ResponseEntity.ok("User deleted successfully.");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Failed to delete user: " + e.getMessage());
        }
    }
}