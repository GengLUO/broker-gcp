package be.kuleuven.dsgt4.broker.services;

import com.google.api.core.ApiFuture;
import com.google.api.core.ApiFutures;
import com.google.cloud.firestore.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import be.kuleuven.dsgt4.broker.domain.TravelPackage;
import io.grpc.netty.shaded.io.netty.handler.timeout.TimeoutException;

@Service
public class TransactionCoordinatorService {

    private static final Logger logger = LoggerFactory.getLogger(TransactionCoordinatorService.class);

    @Autowired
    private Firestore firestore;

    @Autowired
    private BrokerService brokerService;

    public TravelPackage createTravelPackage(String userId) {
        String packageId = generatePackageId();
        TravelPackage travelPackage = new TravelPackage(userId, packageId);
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
        } catch (InterruptedException | ExecutionException e) {
            logger.error("Error storing travel package: {}", e.getMessage());
        }
    }

    // 1. Prepare Phase of the 2PC Booking (Prepare Booking)
    public ApiFuture<Map<String, String>> bookTravelPackage(String packageId, Map<String, Object> bookingDetails) {
        Firestore db = firestore;

        return db.runTransaction(transaction -> {
            logger.info("Booking travel package with packageId: {}", packageId);
            DocumentReference packageRef = db.collection("travelPackages").document(packageId);
            DocumentSnapshot packageSnapshot = transaction.get(packageRef).get();

            if (!packageSnapshot.exists()) {
                throw new IllegalArgumentException("Travel Package with ID " + packageId + " not found");
            }

            // Prepare Phase
            ApiFuture<Map<String, String>> futureMap = ApiFutures.immediateFuture(new HashMap<String, String>());
            // add an actribute action in the bookingDetails to indicate the action and publish the message
            bookingDetails.put("action", "PREPARE");
            ApiFuture<String> flightMessageFuture = brokerService.publishMessage("flight-topic", bookingDetails);
            ApiFuture<String> hotelMessageFuture = brokerService.publishMessage("hotel-topic", bookingDetails);

            // add to the extracted packageSnapshot with a new attribute of flight status being pending
            transaction.update(packageRef, "flightConfirmStatus", false);
            // add to the extracted packageSnapshot with a new attribute of hotel status being pending
            transaction.update(packageRef, "hotelConfirmStatus", false);

            logger.info("message content: {}", bookingDetails);

            String userId = (String) bookingDetails.get("userId");
            // store the packageId in the user's travelPackages list
            DocumentReference userRef = db.collection("users").document(userId);
            transaction.update(userRef, "travelPackages", FieldValue.arrayUnion(packageId));


            futureMap.get().put("flightMessage", flightMessageFuture.get());
            futureMap.get().put("hotelMessage", hotelMessageFuture.get());

            // wait for the futures to complete
            return futureMap.get();
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

            boolean flightConfirmStatus = packageSnapshot.contains("flightConfirmStatus") && packageSnapshot.getBoolean("flightConfirmStatus");
            boolean hotelConfirmStatus = packageSnapshot.contains("hotelConfirmStatus") && packageSnapshot.getBoolean("hotelConfirmStatus");

            if (flightConfirmStatus && hotelConfirmStatus) {
                try {
                    // Wait for the confirmTravelPackage to complete
                    ApiFuture<String> confirmFuture = confirmTravelPackage(packageId, packageSnapshot.getData());
                    // wait for the future to complete
                    confirmFuture.get(3, TimeUnit.MINUTES); // Timeout after 30 seconds
                } catch (ExecutionException | InterruptedException | TimeoutException e) {
                    logger.error("Error confirming travel package: {}", e.getMessage());
                    // If confirmation fails, cancel the travel package
                    ApiFuture<String> cancelFuture = cancelTravelPackage(packageId);
                    try {
                        cancelFuture.get(3, TimeUnit.MINUTES); // Timeout after 30 seconds
                    } catch (ExecutionException | InterruptedException | TimeoutException cancelException) {
                        logger.error("Error cancelling travel package: {}", cancelException.getMessage());
                    }
                }
            } else {
                logger.info("Booking confirmation not yet complete for package: {}", packageId);
            }
            return "Booking confirmation status transaction completed";
        });
    }

    // 2. Commit Phase of the 2PC Booking (Confirm Booking)
    public ApiFuture<String> confirmTravelPackage(String packageId, Map<String, Object> dataBasePackage) {
        Firestore db = firestore;

        return db.runTransaction(transaction -> {
            logger.info("Confirming travel package with packageId: {}", packageId);

            // Commit Phase 
            // construct the bookingDetails from the dataBasePackage
            Map<String, Object> bookingDetails = new HashMap<>();
            bookingDetails.put("packageId", packageId);
            bookingDetails.put("userId", dataBasePackage.get("userId"));

            // Extract hotelId from the hotels list
            List<Map<String, Object>> hotels = (List<Map<String, Object>>) dataBasePackage.get("hotels");
            if (hotels != null && !hotels.isEmpty()) {
                String hotelId = (String) hotels.get(0).get("hotelId");
                bookingDetails.put("hotelId", hotelId);
                String roomsBooked = (String) hotels.get(0).get("roomsBooked");
                bookingDetails.put("roomsBooked", roomsBooked);
            }

            // Extract flightId from the flights list
            List<Map<String, Object>> flights = (List<Map<String, Object>>) dataBasePackage.get("flights");
            if (flights != null && !flights.isEmpty()) {
                String flightId = (String) flights.get(0).get("flightId");
                bookingDetails.put("flightId", flightId);
                String seatsBooked = (String) flights.get(0).get("seatsBooked");
                bookingDetails.put("seatsBooked", seatsBooked);
                String customerName = (String) flights.get(0).get("customerName");
                bookingDetails.put("customerName", customerName);
            }

            // update the action attribute in the bookingDetails to indicate the action and publish the message
            bookingDetails.put("action", "COMMIT");
            ApiFuture<String> flightMessageIdFuture = brokerService.publishMessage("flight-topic", bookingDetails);
            ApiFuture<String> hotelMessageIdFuture = brokerService.publishMessage("hotel-topic", bookingDetails);

            try {
                // Wait for the futures to complete
                String flightMessageId = flightMessageIdFuture.get();
                String hotelMessageId = hotelMessageIdFuture.get();

                logger.info("Flight message ID: {}", flightMessageId);
                logger.info("Hotel message ID: {}", hotelMessageId);
            } catch (Exception e) {
                logger.error("Error during message publishing: ", e);
                throw new RuntimeException("Error during message publishing: " + e.getMessage(), e);
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

            // Commit Phase for Cancellation
            Map<String, Object> bookingDetails = new HashMap<>();
            bookingDetails.put("packageId", packageId);
            bookingDetails.put("userId", packageSnapshot.get("userId"));

            List<Map<String, Object>> hotels = (List<Map<String, Object>>) packageSnapshot.get("hotels");
            if (hotels != null && !hotels.isEmpty()) {
                String hotelId = (String) hotels.get(0).get("hotelId");
                bookingDetails.put("hotelId", hotelId);
                String roomsBooked = (String) hotels.get(0).get("roomsBooked");
                bookingDetails.put("roomsBooked", roomsBooked);
            }

            List<Map<String, Object>> flights = (List<Map<String, Object>>) packageSnapshot.get("flights");
            if (flights != null && !flights.isEmpty()) {
                String flightId = (String) flights.get(0).get("flightId");
                bookingDetails.put("flightId", flightId);
                String seatsBooked = (String) flights.get(0).get("seatsBooked");
                bookingDetails.put("seatsBooked", seatsBooked);
                String customerName = (String) flights.get(0).get("customerName");
                bookingDetails.put("customerName", customerName);
            }
            
            bookingDetails.put("action", "ABORT");
            ApiFuture<String> flightMessageIdFuture = brokerService.publishMessage("flight-topic", bookingDetails);
            ApiFuture<String> hotelMessageIdFuture = brokerService.publishMessage("hotel-topic", bookingDetails);

            // Wait for the futures to complete
            String flightMessageId = flightMessageIdFuture.get();
            String hotelMessageId = hotelMessageIdFuture.get();

            transaction.update(packageRef, "flightConfirmStatus", false);
            transaction.update(packageRef, "seatsBooked", 0);
            transaction.update(packageRef, "flightCancelStatus", true);

            transaction.update(packageRef, "hotelConfirmStatus", false);
            transaction.update(packageRef, "roomsBooked", 0);
            transaction.update(packageRef, "hotelCancelStatus", true);

            return "Travel Package " + packageId + " cancelled initated successfully.";
        });
    }
    
    // Methods for Before Booking
    public void addFlightToPackage(String packageId, Map<String, Object> flightDetails) throws ExecutionException, InterruptedException {
       Firestore db = firestore;
       db.runTransaction(transaction -> {
        // remove the packageId from the flightDetails 
          flightDetails.remove("packageId");
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
            // remove the packageId from the hotelDetails
            hotelDetails.remove("packageId");
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
