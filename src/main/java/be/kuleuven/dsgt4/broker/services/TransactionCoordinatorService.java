package be.kuleuven.dsgt4.broker.services;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import be.kuleuven.dsgt4.broker.domain.TravelPackage;

@Service
public class TransactionCoordinatorService {

    private static final Logger logger = LoggerFactory.getLogger(TransactionCoordinatorService.class);

    @Autowired
    private Firestore firestore;

    @Autowired
    private BrokerService brokerService;

    public TravelPackage createTravelPackage(Map<String, Object> packageDetails) {
        logger.info("Received package details: {}", packageDetails);
        TravelPackage travelPackage = new TravelPackage(packageDetails);
        logger.info("Constructed TravelPackage: {}", travelPackage);
        // Store the travel package in Firestore
        storeTravelPackage(travelPackage);
        return travelPackage;
    }

    private void storeTravelPackage(TravelPackage travelPackage) {
        Firestore db = firestore;
        DocumentReference packageRef = db.collection("travelPackages").document(travelPackage.getPackageId());
        ApiFuture<WriteResult> result = packageRef.set(travelPackage);
        try {
            result.get();  // Ensure the write operation completes
            logger.info("Stored travel package with packageId: {}", travelPackage.getPackageId());
            logger.info("Travel package details: {}", travelPackage);
        } catch (InterruptedException | ExecutionException e) {
            logger.error("Error storing travel package: {}", e.getMessage());
        }
    }

    public ApiFuture<String> bookTravelPackage(String packageId, Map<String, Object> bookingDetails) {
        Firestore db = firestore;

        return db.runTransaction(transaction -> {
            logger.info("Booking travel package with packageId: {}", packageId);
            // Read the travel package document
            DocumentReference packageRef = db.collection("travelPackages").document(packageId);
            DocumentSnapshot packageSnapshot = transaction.get(packageRef).get();
            boolean flgihtStatusExist = packageSnapshot.contains("hotelConfirmStatus");
            boolean hotelStatusExist = packageSnapshot.contains("flightConfirmStatus");
            // Link the travel package to the user by reference
            DocumentReference userRef = db.collection("users").document((String) bookingDetails.get("userId"));
            DocumentSnapshot userSnapshot = transaction.get(userRef).get();
            // Retrieve or initialize the user's travel packages list
            List<DocumentReference> travelPackages = (List<DocumentReference>) userSnapshot.get("travelPackages");

            // for each entry in booking details that corresponds with the entries in the travel package, update the travel package
            for (Map.Entry<String, Object> entry : bookingDetails.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();
                if (packageSnapshot.contains(key)) {
                    transaction.update(packageRef, key, value);
                }
            }

            // Prepare Phase
            bookingDetails.put("action", "PREPARE");
            String flightMessageId = brokerService.publishMessage("flight-topic", bookingDetails);
            if (flightMessageId == null) {
                logger.info("flight-add-requests messageId is null");
                logger.info("message content: {}", bookingDetails);
            }

            if (flgihtStatusExist) {
                transaction.update(packageRef, "flightConfirmStatus", false);
            }

            String hotelMessageId = brokerService.publishMessage("hotel-topic", bookingDetails);
            if (hotelMessageId == null) {
                logger.info("hotel-add-requests messageId is null");
                logger.info("message content: {}", bookingDetails);
            }

            if (hotelStatusExist) {
                transaction.update(packageRef, "hotelConfirmStatus", false);
            }

            if (travelPackages == null) {
                travelPackages = new ArrayList<>();
            }
            travelPackages.add(packageRef);

            // Update the user document with the new list of travel packages
            transaction.update(userRef, "travelPackages", travelPackages);

            return "Travel Package " + packageId + " initiated booking successfully.";
        });
    }

    // 2. Commit Phase of the 2PC Booking (Confirm Booking)
    public ApiFuture<String> checkBookingConfirmation(String packageId, String bookedTypeString) {
        Firestore db = firestore;
        return db.runTransaction((Transaction.Function<String>) transaction -> {
            DocumentReference packageRef = db.collection("travelPackages").document(packageId);
            DocumentSnapshot packageSnapshot = transaction.get(packageRef).get();

            // log the packageSnapshot
            logger.info("Checking booking confirmation for package: id: {}", packageId);

            if (bookedTypeString.equals("flight")) {
                transaction.update(packageRef, "flightConfirmStatus", true);
                if (!packageSnapshot.contains("hotelConfirmStatus")) {
                    transaction.update(packageRef, "hotelConfirmStatus", false);
                }
            } else if (bookedTypeString.equals("hotel")) {
                transaction.update(packageRef, "hotelConfirmStatus", true);
                if (!packageSnapshot.contains("flightConfirmStatus")) {
                    transaction.update(packageRef, "flightConfirmStatus", false);
                }
            }

            if (packageSnapshot.contains("flightConfirmStatus") && packageSnapshot.contains("hotelConfirmStatus")) {
                boolean flightConfirmStatus = packageSnapshot.getBoolean("flightConfirmStatus");
                boolean hotelConfirmStatus = packageSnapshot.getBoolean("hotelConfirmStatus");

                if (flightConfirmStatus && hotelConfirmStatus) {
                    confirmTravelPackage(packageId, packageSnapshot.getData());
                } else {
                    cancelTravelPackage(packageId);
                }
            } else {
                logger.info("Booking confirmation not yet complete for package: {}", packageId);
            }
            return "Booking confirmation status transaction completed";
        });
    }

    // 2. Commit Phase of the 2PC Booking (Confirm Booking)
    public ApiFuture<String> confirmTravelPackage(String packageId, Map<String, Object> bookingDetails) {
        Firestore db = firestore;

        return db.runTransaction(transaction -> {
            logger.info("Confirming travel package with packageId: {}", packageId);

            // Commit Phase 

            // update the action attribute in the bookingDetails to indicate the action and publish the message
            bookingDetails.put("action", "COMMIT");
            String flightMessageId = brokerService.publishMessage("flight-topic", bookingDetails);
            if (flightMessageId == null) {
                logger.error("flight-booking-requests messageId is null");
                logger.error("message content: {}", bookingDetails);
            }

            String hoteltMessageId = brokerService.publishMessage("hotel-topic", bookingDetails);
            if (hoteltMessageId == null) {
                logger.error(" hotel-booking-requests messageId is null");
                logger.error("message content: {}", bookingDetails);
            }

            return "Travel Package " + packageId + " confirmed successfully.";
        });
    }

    // 3. Abort Phase of the 2PC Booking (Cancel Booking)
    public ApiFuture<String> cancelTravelPackage(String packageId) {
        Firestore db = firestore;

        return db.runTransaction(transaction -> {
            DocumentReference packageRef = db.collection("travelPackages").document(packageId);
            DocumentSnapshot packageSnapshot = transaction.get(packageRef).get();

            if (!packageSnapshot.exists()) {
                throw new IllegalArgumentException("Travel Package with ID " + packageId + " not found");
            }

            List<Map<String, Object>> flights = (List<Map<String, Object>>) packageSnapshot.get("flights");
            List<Map<String, Object>> hotels = (List<Map<String, Object>>) packageSnapshot.get("hotels");

            // Commit Phase for Cancellation
            // update the action attribute in the bookingDetails to indicate the action and publish the message
            Map<String, Object> bookingDetails = packageSnapshot.getData();
            bookingDetails.put("action", "ABORT");
            String flightMessageId = brokerService.publishMessage("flight-cancel-request", bookingDetails);
            if (flightMessageId == null) {
                logger.error("flight-cancel-requests messageId is null");
                logger.error("message content: {}", bookingDetails);
            }

            transaction.update(packageRef, "flightConfirmStatus", false);
            // set the number of seats booked in the flights to zero
            if (flights != null) {
                for (Map<String, Object> flight : flights) {
                    String flightId = (String) flight.get("flightId");
                    DocumentReference flightRef = db.collection("flights").document(flightId);
                    transaction.update(flightRef, "seatsBooked", 0);
                }
            }

            String hotelMessageId = brokerService.publishMessage("hotel-cancel-request", bookingDetails);
            if (hotelMessageId == null) {
                logger.error("flight-cancel-requests message is null");
                logger.error("message content: {}", bookingDetails);
            }

            transaction.update(packageRef, "hotelConfirmStatus", false);
            // aet the number of rooms booked in the hotels to zero
            if (hotels != null) {
                for (Map<String, Object> hotel : hotels) {
                    String hotelId = (String) hotel.get("hotelId");
                    DocumentReference hotelRef = db.collection("hotels").document(hotelId);
                    transaction.update(hotelRef, "roomsBooked", 0);
                }
            }
            return "Travel Package " + packageId + " cancelled successfully.";
        });
    }

    // Methods for Before Booking
    public void addFlightToPackage(String packageId, Map<String, Object> flightDetails) throws ExecutionException, InterruptedException {
       Firestore db = firestore;
       db.runTransaction(transaction -> {
          DocumentReference packageRef = db.collection("travelPackages").document(packageId);
          transaction.update(packageRef, "flights", FieldValue.arrayUnion(flightDetails));
              return null;
       }).get();
    }

    public void removeFlightFromPackage( String packageId, String flightId) throws ExecutionException, InterruptedException {
        Firestore db = firestore;
        db.runTransaction(transaction -> {
            DocumentReference packageRef = db.collection("travelPackages").document(packageId);
            DocumentSnapshot packageSnapshot = transaction.get(packageRef).get();
            List<Map<String, Object>> flights = (List<Map<String, Object>>) packageSnapshot.get("flights");
            flights.removeIf(flight -> flight.get("flightId").equals(flightId));
            transaction.update(packageRef, "flights", flights);
            return null;
        }).get();
    }

    public void addHotelToPackage(String packageId, Map<String, Object> hotelDetails) throws ExecutionException, InterruptedException {
        Firestore db = firestore;
        db.runTransaction(transaction -> {
            DocumentReference packageRef = db.collection("travelPackages").document(packageId);
            transaction.update(packageRef, "hotels", FieldValue.arrayUnion(hotelDetails));
            return null;
        }).get();
    }

    public void removeHotelFromPackage(String packageId, String hotelId) throws ExecutionException, InterruptedException {
        Firestore db = firestore;
        db.runTransaction(transaction -> {
            DocumentReference packageRef = db.collection("travelPackages").document(packageId);
            DocumentSnapshot packageSnapshot = transaction.get(packageRef).get();
            List<Map<String, Object>> hotels = (List<Map<String, Object>>) packageSnapshot.get("hotels");
            hotels.removeIf(hotel -> hotel.get("hotelId").equals(hotelId));
            transaction.update(packageRef, "hotels", hotels);
            return null;
        }).get();
    }

    public void addCustomerToPackage(String packageId, Map<String, Object> customerDetails) throws ExecutionException, InterruptedException {
        Firestore db = firestore;
        db.runTransaction(transaction -> {
            DocumentReference packageRef = db.collection("travelPackages").document(packageId);
            transaction.update(packageRef, "customers", FieldValue.arrayUnion(customerDetails));
            return null;
        }).get();
    }

    public void removeCustomerFromPackage(String packageId, String customerId) throws ExecutionException, InterruptedException {
        Firestore db = firestore;
        db.runTransaction(transaction -> {
            DocumentReference packageRef = db.collection("travelPackages").document(packageId);
            DocumentSnapshot packageSnapshot = transaction.get(packageRef).get();
            List<Map<String, Object>> customers = (List<Map<String, Object>>) packageSnapshot.get("customers");
            customers.removeIf(customer -> customer.get("id").equals(customerId));
            transaction.update(packageRef, "customers", customers);
            return null;
        }).get();
    }

    // Methods for After Booking
    public void updateFlightInPackage(String packageId, Map<String, Object> flightDetails) throws ExecutionException, InterruptedException, IOException {
        Firestore db = firestore;
        db.runTransaction(transaction -> {
            DocumentReference packageRef = db.collection("travelPackages").document(packageId);
            DocumentSnapshot packageSnapshot = transaction.get(packageRef).get();
            brokerService.publishMessage("flight-update-requests", flightDetails);
            List<Map<String, Object>> flights = (List<Map<String, Object>>) packageSnapshot.get("flights");
            flights.removeIf(flight -> flight.get("flightId").equals(flightDetails.get("flightId")));
            flights.add(flightDetails);
            transaction.update(packageRef, "flights", flights);
            return null;
        }).get();
    }

    public void updateHotelInPackage(String packageId, Map<String, Object> hotelDetails) throws ExecutionException, InterruptedException, IOException {
        Firestore db = firestore;
        db.runTransaction(transaction -> {
            DocumentReference packageRef = db.collection("travelPackages").document(packageId);
            DocumentSnapshot packageSnapshot = transaction.get(packageRef).get();
            brokerService.publishMessage("hotel-update-requests", hotelDetails);
            List<Map<String, Object>> hotels = (List<Map<String, Object>>) packageSnapshot.get("hotels");
            hotels.removeIf(hotel -> hotel.get("hotelId").equals(hotelDetails.get("hotelId")));
            hotels.add(hotelDetails);
            transaction.update(packageRef, "hotels", hotels);
            return null;
        }).get();
    }

    public void updateCustomerInPackage(String packageId, Map<String, Object> customerDetails) {
        Firestore db = firestore;
        db.runTransaction(transaction -> {
            DocumentReference packageRef = db.collection("travelPackages").document(packageId);
            DocumentSnapshot packageSnapshot = transaction.get(packageRef).get();
            List<Map<String, Object>> customers = (List<Map<String, Object>>) packageSnapshot.get("customers");
            customers.removeIf(customer -> customer.get("id").equals(customerDetails.get("id")));
            customers.add(customerDetails);
            transaction.update(packageRef, "customers", customers);
            return null;
        });
    }

    private String generatePackageId() {
        return "package-" + System.currentTimeMillis();
    }


}
