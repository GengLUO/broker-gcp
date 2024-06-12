package be.kuleuven.dsgt4.broker.controller;

import be.kuleuven.dsgt4.auth.WebSecurityConfig;
import be.kuleuven.dsgt4.broker.domain.User;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AuthorizationServiceException;
import org.springframework.web.bind.annotation.*;


import java.util.*;
import java.util.concurrent.ExecutionException;

// Add the controller.
@RestController
@RequestMapping("/api") //指定了这个控制器处理的URL前缀为/api
class FirestoreController {

    @Autowired
    private Firestore firestore;

    @GetMapping("/hello")
    public String hello() {
        System.out.println("Inside hello");
        return "hello world!";
    }

    @GetMapping("/whoami")
    public User whoami() throws InterruptedException, ExecutionException {
        var user = WebSecurityConfig.getUser();
        if (!user.isManager()) throw new AuthorizationServiceException("You are not a manager");
        return user;
    }

    /** API Endpoint */
    /** Basic CRUD Operations */
    @PostMapping("/add")
    public ApiFuture<WriteResult> addDocument(@RequestBody Map<String, Object> data) {
        DocumentReference docRef = firestore.collection("testCollection").document();
        return docRef.set(data);
    }

    @GetMapping("/get/{id}")
    public ApiFuture<DocumentSnapshot> getDocument(@PathVariable String id) {
        DocumentReference docRef = firestore.collection("testCollection").document(id);
        return docRef.get();
    }

    @PutMapping("/update/{id}")
    public ApiFuture<WriteResult> updateDocument(@PathVariable String id, @RequestBody Map<String, Object> data) {
        DocumentReference docRef = firestore.collection("testCollection").document(id);
        return docRef.update(data);
    }

    @DeleteMapping("/delete/{id}")
    public ApiFuture<WriteResult> deleteDocument(@PathVariable String id) {
        DocumentReference docRef = firestore.collection("testCollection").document(id);
        return docRef.delete();
    }

    /** Specified Methods Using ApiFuture */
    @PostMapping("/addTravelPackage")
    public ApiFuture<WriteResult> addTravelPackage(@RequestBody Map<String, Object> data) {
        DocumentReference docRef = firestore.collection("travelPackages").document();
        return docRef.set(data);
    }

    @GetMapping("/getTravelPackages/{customerId}")
    public ApiFuture<QuerySnapshot> getTravelPackagesByCustomer(@PathVariable String customerId) {
        return firestore.collection("travelPackages")
                        .whereEqualTo("customerId", customerId)
                        .get();
    }

    @PostMapping("/bookTravelPackage/{packageId}")
    public ApiFuture<String> bookTravelPackage(@PathVariable String packageId, @RequestBody Map<String, Object> bookingDetails) {
        Firestore db = firestore;
    
        return db.runTransaction(transaction -> {
            DocumentReference packageRef = db.collection("travelPackages").document(packageId);
            DocumentSnapshot packageSnapshot = transaction.get(packageRef).get();
    
            if (!packageSnapshot.exists()) {
                throw new IllegalArgumentException("Travel Package with ID " + packageId + " not found");
            }
    
            // Extracting details from packageSnapshot
            List<String> flightIds = (List<String>) packageSnapshot.get("flightIds");
            List<String> hotelIds = (List<String>) packageSnapshot.get("hotelIds");
    
            // Updating flights
            for (String flightId : flightIds) {
                DocumentReference flightRef = db.collection("flights").document(flightId);
                transaction.update(flightRef, "booked", true);
            }
    
            // Updating hotels
            for (String hotelId : hotelIds) {
                DocumentReference hotelRef = db.collection("hotels").document(hotelId);
                transaction.update(hotelRef, "bookedRooms", FieldValue.increment((Integer) bookingDetails.get("roomsBooked")));
            }
    
            return "Travel Package " + packageId + " booked successfully.";
        });
    }

    @PutMapping("/updateTravelPackage/{id}")
    public ApiFuture<WriteResult> updateTravelPackage(@PathVariable String id, @RequestBody Map<String, Object> data) {
        DocumentReference docRef = firestore.collection("travelPackages").document(id);
        return docRef.update(data);
    }

    @DeleteMapping("/deleteTravelPackage/{id}")
    public ApiFuture<WriteResult> deleteTravelPackage(@PathVariable String id) {
        DocumentReference docRef = firestore.collection("travelPackages").document(id);
        return docRef.delete();
    }

    /** User specific methods */
    @PostMapping("/updateUserProfile/{userId}")
    public ApiFuture<String> updateUserProfile(@PathVariable String userId, @RequestBody Map<String, Object> newDetails) {
        return firestore.runTransaction(transaction -> {
            DocumentReference userRef = firestore.collection("users").document(userId);
            DocumentSnapshot userSnapshot = transaction.get(userRef).get();

            if (!userSnapshot.exists()) {
                throw new IllegalArgumentException("User with ID " + userId + " not found");
            }

            transaction.update(userRef, "email", newDetails.get("email"));

            DocumentReference settingsRef = firestore.collection("userSettings").document(userId);
            transaction.update(settingsRef, "preferences", newDetails.get("preferences"));

            return "User profile for " + userId + " updated successfully.";
        });
    }

}
