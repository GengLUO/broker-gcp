package be.kuleuven.dsgt4.broker.controllers;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AuthorizationServiceException;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.concurrent.ExecutionException;

import be.kuleuven.dsgt4.auth.WebSecurityConfig;
import be.kuleuven.dsgt4.broker.domain.Customer;
import be.kuleuven.dsgt4.broker.domain.User;
import be.kuleuven.dsgt4.broker.services.TransactionCoordinatorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api")
class FirestoreController {

    private static final Logger logger = LoggerFactory.getLogger(FirestoreController.class);

    @Autowired
    private Firestore firestore;

    @Autowired
    private TransactionCoordinatorService transactionCoordinatorService;

    @GetMapping("/hello")
    public String hello() {
        logger.info("Received request at /hello");
        return "Welcome to Travel Broker API!";
    }

    @GetMapping("/whoami")
    public User whoami() throws InterruptedException, ExecutionException {
        var user = WebSecurityConfig.getUser();
        if (!user.isManager()) throw new AuthorizationServiceException("You are not a manager");
        logger.info("Returning user details: {}", user.getEmail());
        return user;
    }

    @PostMapping("/updateUserProfile/{userId}")
    public ApiFuture<String> updateUserProfile(@PathVariable String userId, @RequestBody Map<String, Object> newDetails) {
        logger.info("Updating user profile for userId: {}", userId);
        return firestore.runTransaction(transaction -> {
            DocumentReference userRef = firestore.collection("users").document(userId);
            DocumentSnapshot userSnapshot = transaction.get(userRef).get();

            if (!userSnapshot.exists()) {
                throw new IllegalArgumentException("User with ID " + userId + " not found");
            }

            if (newDetails.containsKey("email")) {
                transaction.update(userRef, "email", newDetails.get("email"));
            }

            if (newDetails.containsKey("role")) {
                transaction.update(userRef, "role", newDetails.get("role"));
            }

            return "User profile for " + userId + " updated successfully.";
        });
    }

    @PostMapping("/addCustomer/{userId}")
    public ApiFuture<WriteResult> addCustomer(@PathVariable String userId, @RequestBody Customer customer) {
        logger.info("Adding customer for userId: {}", userId);
        DocumentReference docRef = firestore.collection("users").document(userId).collection("customers").document();
        return docRef.set(customer);
    }

    @GetMapping("/getCustomers/{userId}")
    public ApiFuture<QuerySnapshot> getCustomers(@PathVariable String userId) {
        logger.info("Fetching customers for userId: {}", userId);
        return firestore.collection("users").document(userId).collection("customers").get();
    }

    @PutMapping("/updateCustomer/{userId}/{customerId}")
    public ApiFuture<WriteResult> updateCustomer(@PathVariable String userId, @PathVariable String customerId, @RequestBody Map<String, Object> updates) {
        logger.info("Updating customer for userId: {}, customerId: {}", userId, customerId);
        DocumentReference docRef = firestore.collection("users").document(userId).collection("customers").document(customerId);
        return docRef.update(updates);
    }

    @DeleteMapping("/deleteCustomer/{userId}/{customerId}")
    public ApiFuture<WriteResult> deleteCustomer(@PathVariable String userId, @PathVariable String customerId) {
        logger.info("Deleting customer for userId: {}, customerId: {}", userId, customerId);
        DocumentReference docRef = firestore.collection("users").document(userId).collection("customers").document(customerId);
        return docRef.delete();
    }

    @GetMapping("/getTravelPackages/{customerId}")
    public ApiFuture<QuerySnapshot> getTravelPackagesByCustomer(@PathVariable String customerId) {
        logger.info("Fetching travel packages for customerId: {}", customerId);
        return firestore.collection("travelPackages").whereEqualTo("customerId", customerId).get();
    }

    @PostMapping("/addTravelPackage")
    public ApiFuture<WriteResult> addTravelPackage(@RequestBody Map<String, Object> data) {
        logger.info("Adding new travel package");
        return transactionCoordinatorService.addTravelPackage(data);
    }

    @PostMapping("/bookTravelPackage/{packageId}")
    public ApiFuture<String> bookTravelPackage(@PathVariable String packageId, @RequestBody Map<String, Object> bookingDetails) {
        logger.info("Booking travel package with packageId: {}", packageId);
        return transactionCoordinatorService.bookTravelPackage(packageId, bookingDetails);
    }

    @PutMapping("/updateTravelPackage/{id}")
    public ApiFuture<WriteResult> updateTravelPackage(@PathVariable String id, @RequestBody Map<String, Object> data) {
        logger.info("Updating travel package with id: {}", id);
        return transactionCoordinatorService.updateTravelPackage(id, data);
    }

    @DeleteMapping("/deleteTravelPackage/{id}")
    public ApiFuture<WriteResult> deleteTravelPackage(@PathVariable String id) {
        logger.info("Deleting travel package with id: {}", id);
        return transactionCoordinatorService.deleteTravelPackage(id);
    }

}