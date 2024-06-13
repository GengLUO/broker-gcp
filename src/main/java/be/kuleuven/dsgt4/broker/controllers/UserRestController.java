package be.kuleuven.dsgt4.broker.controllers;

import be.kuleuven.dsgt4.auth.WebSecurityConfig;
import be.kuleuven.dsgt4.broker.domain.User;
import be.kuleuven.dsgt4.broker.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AuthorizationServiceException;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;
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
        if (!user.isManager()) throw new AuthorizationServiceException("You are not a manager");

        UUID buuid = UUID.randomUUID();
//        TODO: According to the code provided, we need to store the user message. Do we need to use it?
//        UserMessage b = new UserMessage(buuid, LocalDateTime.now(), user.getRole(), user.getEmail());
//        this.db.collection("usermessages").document(b.getId().toString()).set(b.toDoc()).get();

        return user;
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
