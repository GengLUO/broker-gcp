Yes, you are correct. The user should be able to update or delete travel package details, including flights or hotels. These actions should be communicated with the suppliers to ensure consistency across the system. Here's how to implement this functionality:

### Updated `BookingController` for Update and Delete Operations

Add methods to `BookingController` to handle updates and deletions of travel packages. These methods should communicate with the `TransactionCoordinatorService` to manage these operations.

```java
package be.kuleuven.dsgt4.broker.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import be.kuleuven.dsgt4.broker.services.BrokerPublisherService;
import be.kuleuven.dsgt4.broker.services.TransactionCoordinatorService;
import be.kuleuven.dsgt4.broker.services.PaymentService;

import java.util.Map;

@RestController
@RequestMapping("/bookings")
public class BookingController {

    private final BrokerPublisherService publisherService;
    private final TransactionCoordinatorService transactionCoordinatorService;
    private final PaymentService paymentService;

    @Autowired
    public BookingController(BrokerPublisherService publisherService, TransactionCoordinatorService transactionCoordinatorService, PaymentService paymentService) {
        this.publisherService = publisherService;
        this.transactionCoordinatorService = transactionCoordinatorService;
        this.paymentService = paymentService;
    }

    @GetMapping("/test-publish/{topic}")
    public ResponseEntity<?> testPublish(@PathVariable String topic) {
        try {
            String messageId = publisherService.publishMessage(topic, "Test message");
            return ResponseEntity.ok("Test message published successfully. Message ID: " + messageId);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to publish test message: " + e.getMessage());
        }
    }

    @PostMapping("/hotels")
    public ResponseEntity<?> createHotelBooking(@RequestBody String bookingDetails) {
        try {
            String messageId = publisherService.publishMessage("hotel-booking-requests", bookingDetails);
            EntityModel<String> resource = bookingToEntityModel("hotel", messageId, bookingDetails);
            return ResponseEntity.created(WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(BookingController.class)
                    .createHotelBooking(bookingDetails)).toUri()).body(resource);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error processing hotel booking: " + e.getMessage());
        }
    }

    @PostMapping("/flights")
    public ResponseEntity<?> createFlightBooking(@RequestBody String bookingDetails) {
        try {
            String messageId = publisherService.publishMessage("flight-booking-requests", bookingDetails);
            EntityModel<String> resource = bookingToEntityModel("flight", messageId, bookingDetails);
            return ResponseEntity.created(WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(BookingController.class)
                    .createFlightBooking(bookingDetails)).toUri()).body(resource);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error processing flight booking: " + e.getMessage());
        }
    }

    @PostMapping("/book")
    public ResponseEntity<?> bookTravelPackage(@RequestBody Map<String, Object> bookingDetails) {
        try {
            String packageId = (String) bookingDetails.get("packageId");
            String messageId = transactionCoordinatorService.bookTravelPackage(packageId, bookingDetails).get();
            EntityModel<String> resource = bookingToEntityModel("travelPackage", messageId, bookingDetails.toString());
            return ResponseEntity.created(WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(BookingController.class)
                    .bookTravelPackage(bookingDetails)).toUri()).body(resource);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error processing travel package booking: " + e.getMessage());
        }
    }

    @PostMapping("/pay")
    public ResponseEntity<?> processPayment(@RequestBody Map<String, Object> paymentDetails) {
        try {
            String transactionId = (String) paymentDetails.get("transactionId");
            boolean success = paymentService.processPayment(transactionId, paymentDetails);
            if (success) {
                return ResponseEntity.ok("Payment processed successfully");
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Payment failed");
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error processing payment: " + e.getMessage());
        }
    }

    @PutMapping("/{type}/{id}")
    public ResponseEntity<?> updateBooking(@PathVariable String type, @PathVariable String id, @RequestBody Map<String, Object> updateDetails) {
        try {
            transactionCoordinatorService.updateTravelPackage(id, updateDetails).get();
            return ResponseEntity.ok("Booking updated successfully.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error updating booking: " + e.getMessage());
        }
    }

    @DeleteMapping("/{type}/{id}")
    public ResponseEntity<?> deleteBooking(@PathVariable String type, @PathVariable String id) {
        try {
            transactionCoordinatorService.deleteTravelPackage(id).get();
            return ResponseEntity.ok("Booking deleted successfully.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error deleting booking: " + e.getMessage());
        }
    }

    @GetMapping("/{type}/{id}")
    public ResponseEntity<?> retrieveBooking(@PathVariable String type, @PathVariable String id) {
        String bookingDetails = "Booking details for " + type + " ID: " + id;
        return ResponseEntity.ok(bookingToEntityModel(type, id, bookingDetails));
    }

    private EntityModel<String> bookingToEntityModel(String type, String messageId, String bookingDetails) {
        return EntityModel.of("Booking request submitted successfully. Message ID: " + messageId,
                WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(BookingController.class).retrieveBooking(type, messageId)).withSelfRel(),
                WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(BookingController.class).deleteBooking(type, messageId)).withRel("delete-booking"),
                WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(BookingController.class).updateBooking(type, messageId, null)).withRel("update-booking")
        );
    }
}
```

### Update `TransactionCoordinatorService` for Update and Delete Operations

Ensure that update and delete operations also communicate with the suppliers and handle these operations through the `TransactionCoordinatorService`.

```java
package be.kuleuven.dsgt4.broker.services;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import be.kuleuven.dsgt4.broker.domain.BookingResponse;
import be.kuleuven.dsgt4.broker.domain.BookingTransaction;

@Service
public class TransactionCoordinatorService {

    private static final Logger logger = LoggerFactory.getLogger(TransactionCoordinatorService.class);

    @Autowired
    private Firestore firestore;

    @Autowired
    private PBFTService pbftService;

    public ApiFuture<WriteResult> addTravelPackage(Map<String, Object> data) {
        logger.info("Adding travel package");
        Firestore db = firestore;

        return db.runTransaction(transaction -> {
            DocumentReference docRef = db.collection("travelPackages").document();
            transaction.set(docRef, data);
            return null;
        });
    }

    public ApiFuture<String> bookTravelPackage(String packageId, Map<String, Object> bookingDetails) {
        Firestore db = firestore;

        return db.runTransaction(transaction -> {
            logger.info("Booking travel package with packageId: {}", packageId);
            DocumentReference packageRef = db.collection("travelPackages").document(packageId);
            DocumentSnapshot packageSnapshot = transaction.get(packageRef).get();

            if (!packageSnapshot.exists()) {
                throw new IllegalArgumentException("Travel Package with ID " + packageId + " not found");
            }

            List<String> flightIds = (List<String>) packageSnapshot.get("flightIds");
            List<String> hotelIds = (List<String>) packageSnapshot.get("hotelIds");

            for (String flightId : flightIds) {
                DocumentReference flightRef = db.collection("flights").document(flightId);
                transaction.update(flightRef, "booked", true);
            }

            for (String hotelId : hotelIds) {
                DocumentReference hotelRef = db.collection("hotels").document(hotelId);
                transaction.update(hotelRef, "bookedRooms", FieldValue.increment((Integer) bookingDetails.get("roomsBooked")));
            }

            String userId = (String) bookingDetails.get("userId");
            DocumentReference userRef = db.collection("users").document(userId).collection("bookings").document();
            transaction.set(userRef, bookingDetails);

            // Initiate PBFT consensus for the booking
            if (pbftService.initiateConsensus(packageId + bookingDetails.toString())) {
                return "Travel Package " + packageId + " booked successfully.";
            } else {
                throw new IllegalStateException("Consensus not achieved for booking package ID: " + packageId);
            }
        });
    }

    public ApiFuture<WriteResult> updateTravelPackage(String id, Map<String, Object> data) {
        logger.info("Updating travel package with id: {}", id);
        Firestore db = firestore;

       

 return db.runTransaction(transaction -> {
            DocumentReference docRef = db.collection("travelPackages").document(id);
            DocumentSnapshot docSnapshot = transaction.get(docRef).get();

            if (!docSnapshot.exists()) {
                throw new IllegalArgumentException("Travel Package with ID " + id + " not found");
            }

            for (Map.Entry<String, Object> entry : data.entrySet()) {
                transaction.update(docRef, entry.getKey(), entry.getValue());
            }

            // Notify suppliers about the update
            notifySuppliersAboutUpdate(data);

            transaction.update(docRef, data);
            return null;
        });
    }

    public ApiFuture<WriteResult> deleteTravelPackage(String id) {
        logger.info("Deleting travel package with id: {}", id);
        Firestore db = firestore;

        return db.runTransaction(transaction -> {
            DocumentReference docRef = db.collection("travelPackages").document(id);
            DocumentSnapshot docSnapshot = transaction.get(docRef).get();

            if (!docSnapshot.exists()) {
                throw new IllegalArgumentException("Travel Package with ID " + id + " not found");
            }

            // Notify suppliers about the deletion
            notifySuppliersAboutDeletion(id);

            transaction.delete(docRef);
            return null;
        });
    }

    private void notifySuppliersAboutUpdate(Map<String, Object> updateDetails) {
        // Implement logic to notify suppliers about the update
    }

    private void notifySuppliersAboutDeletion(String packageId) {
        // Implement logic to notify suppliers about the deletion
    }

    public void processBookingResponse(String message) {
        BookingResponse bookingResponse = parseMessage(message);

        ApiFuture<DocumentSnapshot> future = firestore.collection("transactions").document(bookingResponse.getTransactionId()).get();
        try {
            DocumentSnapshot document = future.get();
            if (document.exists()) {
                BookingTransaction bookingTransaction = document.toObject(BookingTransaction.class);

                if (bookingResponse.isSuccess()) {
                    commitTransaction(bookingTransaction);
                } else {
                    abortTransaction(bookingTransaction);
                }
            } else {
                logger.error("Transaction not found for ID: " + bookingResponse.getTransactionId());
            }
        } catch (InterruptedException | ExecutionException e) {
            logger.error("Error processing booking response: ", e);
        }
    }

    private BookingResponse parseMessage(String message) {
        return new BookingResponse("transactionId", true);
    }

    private void commitTransaction(BookingTransaction bookingTransaction) {
        Firestore db = firestore;
        db.runTransaction(transaction -> {
            for (String flightId : bookingTransaction.getFlightIds()) {
                DocumentReference flightRef = db.collection("flights").document(flightId);
                transaction.update(flightRef, "status", "confirmed");
            }

            for (String hotelId : bookingTransaction.getHotelIds()) {
                DocumentReference hotelRef = db.collection("hotels").document(hotelId);
                transaction.update(hotelRef, "status", "confirmed");
            }

            DocumentReference userRef = db.collection("users").document(bookingTransaction.getUserId()).collection("bookings").document(bookingTransaction.getTransactionId());
            transaction.update(userRef, "status", "confirmed");

            return null;
        }).addOnSuccessListener(aVoid -> logger.info("Transaction committed successfully"))
          .addOnFailureListener(e -> logger.error("Transaction commit failed", e));
    }

    private void abortTransaction(BookingTransaction bookingTransaction) {
        Firestore db = firestore;
        db.runTransaction(transaction -> {
            for (String flightId : bookingTransaction.getFlightIds()) {
                DocumentReference flightRef = db.collection("flights").document(flightId);
                transaction.update(flightRef, "status", "available");
            }

            for (String hotelId : bookingTransaction.getHotelIds()) {
                DocumentReference hotelRef = db.collection("hotels").document(hotelId);
                transaction.update(hotelRef, "status", "available");
            }

            DocumentReference userRef = db.collection("users").document(bookingTransaction.getUserId()).collection("bookings").document(bookingTransaction.getTransactionId());
            transaction.update(userRef, "status", "failed");

            return null;
        }).addOnSuccessListener(aVoid -> logger.info("Transaction aborted successfully"))
          .addOnFailureListener(e -> logger.error("Transaction abort failed", e));
    }
}
```

In this updated implementation:
- `BookingController` has endpoints for updating and deleting bookings.
- `TransactionCoordinatorService` has methods to update and delete travel packages and notify suppliers of these changes.

### Notifying Suppliers

You can use `BrokerPublisherService` to notify suppliers about updates and deletions by publishing messages to the Pub/Sub system.

### `notifySuppliersAboutUpdate` and `notifySuppliersAboutDeletion` Implementations

```java
private void notifySuppliersAboutUpdate(Map<String, Object> updateDetails) {
    try {
        // Example: Notify hotel and flight suppliers
        if (updateDetails.containsKey("hotelUpdate")) {
            publisherService.publishMessage("hotel-update", updateDetails.get("hotelUpdate").toString());
        }
        if (updateDetails.containsKey("flightUpdate")) {
            publisherService.publishMessage("flight-update", updateDetails.get("flightUpdate").toString());
        }
    } catch (Exception e) {
        logger.error("Error notifying suppliers about update: ", e);
    }
}

private void notifySuppliersAboutDeletion(String packageId) {
    try {
        // Example: Notify hotel and flight suppliers about the deletion
        publisherService.publishMessage("hotel-deletion", packageId);
        publisherService.publishMessage("flight-deletion", packageId);
    } catch (Exception e) {
        logger.error("Error notifying suppliers about deletion: ", e);
    }
}
```

### Update Suppliers to Handle Updates and Deletions

Ensure that the hotel and flight services are updated to handle these new messages from the Pub/Sub system. This involves subscribing to the relevant topics and implementing the necessary logic to update or delete the booking details.

With these updates, the system will be able to handle the full range of CRUD operations for travel packages, communicate with suppliers, and maintain consistency and fault tolerance using PBFT.