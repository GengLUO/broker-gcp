package be.kuleuven.dsgt4.broker.services;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
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
    public ApiFuture<String> bookTravelPackage(String packageId, Map<String, Object> bookingDetails) {
        Firestore db = firestore;

        return db.runTransaction(transaction -> {
            logger.info("Booking travel package with packageId: {}", packageId);
            DocumentReference packageRef = db.collection("travelPackages").document(packageId);
            DocumentSnapshot packageSnapshot = transaction.get(packageRef).get();

            if (!packageSnapshot.exists()) {
                throw new IllegalArgumentException("Travel Package with ID " + packageId + " not found");
            }

            List<Map<String, Object>> flights = (List<Map<String, Object>>) packageSnapshot.get("flights");
            List<Map<String, Object>> hotels = (List<Map<String, Object>>) packageSnapshot.get("hotels");

            // Print flights
            if (flights != null) {
                System.out.println("Flights:");
                for (Map<String, Object> flight : flights) {
                    System.out.println(flight);
                }
            } else {
                System.out.println("flights is Null.");
            }

            // Print hotels
            if (hotels != null) {
                System.out.println("Hotels:");
                for (Map<String, Object> hotel : hotels) {
                    System.out.println(hotel);
                }
            } else {
                System.out.println("hotels is Null.");
            }

            // Prepare Phase
            // add an actribute action in the bookingDetails to indicate the action and publish the message
            bookingDetails.put("action", "PREPARE");
            String flightMessageId = brokerService.publishMessage("flight-topic", bookingDetails);
            if (flightMessageId == null) {
                logger.error("Failed to publish flight-add-requests message");
                // reutrn null to indicate messgae publish failure (could not be retried)
                return null;
            }

            for (Map<String, Object> flight : flights) {
                String flightId = (String) flight.get("flightId");
                DocumentReference flightRef = db.collection("flights").document(flightId);
                transaction.update(flightRef, "status", "pending");
            }

            String hotelMessageId = brokerService.publishMessage("hotel-topic", bookingDetails);
            if (hotelMessageId == null) {
                logger.error("Failed to publish hotel-add-requests message");
                // reutrn null to indicate messgae publish failure (could not be retried)
                return null;
            }
            for (Map<String, Object> hotel : hotels) {
                String hotelId = (String) hotel.get("hotelId");
                DocumentReference hotelRef = db.collection("hotels").document(hotelId);
                transaction.update(hotelRef, "status", "pending");
            }

            String userId = (String) bookingDetails.get("userId");
            DocumentReference userRef = db.collection("users").document(userId).collection("travelPackages").document(packageId);
            transaction.set(userRef, bookingDetails);

            return "Travel Package " + packageId + " booked successfully.";
        });
    }

    // 2. Commit Phase of the 2PC Booking (Confirm Booking)
    public ApiFuture<String> checkBookingConfirmation(String packageId, String bookedTypeString) {
        Firestore db = firestore;
        return db.runTransaction((Transaction.Function<String>) transaction -> {
            DocumentReference packageRef = db.collection("travelPackages").document(packageId);
            DocumentSnapshot packageSnapshot = transaction.get(packageRef).get();

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
                    cancelTravelPackage((String) packageSnapshot.get("userId"), packageId);
                }
            } else {
                logger.info("Booking confirmation not yet complete for package: {}", packageId);
            }
            return "Transaction completed";
        });
    }

    // 2. Commit Phase of the 2PC Booking (Confirm Booking)
    public ApiFuture<String> confirmTravelPackage(String packageId, Map<String, Object> bookingDetails) {
        Firestore db = firestore;

        return db.runTransaction(transaction -> {
            logger.info("Confirming travel package with packageId: {}", packageId);
            DocumentReference packageRef = db.collection("travelPackages").document(packageId);
            DocumentSnapshot packageSnapshot = transaction.get(packageRef).get();

            if (!packageSnapshot.exists()) {
                throw new IllegalArgumentException("Travel Package with ID " + packageId + " not found");
            }

            List<Map<String, Object>> flights = (List<Map<String, Object>>) packageSnapshot.get("flights");
            List<Map<String, Object>> hotels = (List<Map<String, Object>>) packageSnapshot.get("hotels");

            // Commit Phase 
            // update the action attribute in the bookingDetails to indicate the action and publish the message
            bookingDetails.put("action", "COMMIT");
            String flightMessageId = brokerService.publishMessage("flight-topic", bookingDetails);
            if (flightMessageId == null) {
                logger.error("Failed to publish flight-booking-requests message");
                // reutrn null to indicate message failure (could not be retried)
                return null;
            }
            for (Map<String, Object> flight : flights) {
                String flightId = (String) flight.get("flightId");
                DocumentReference flightRef = db.collection("flights").document(flightId);
                transaction.update(flightRef, "status", "committed");
            }

            String hoteltMessageId = brokerService.publishMessage("hotel-topic", bookingDetails);
            if (hoteltMessageId == null) {
                logger.error("Failed to publish flight-booking-requests message");
                // reutrn null to indicate message failure (could not be retried)
                return null;
            }
            for (Map<String, Object> hotel : hotels) {
                String hotelId = (String) hotel.get("hotelId");
                DocumentReference hotelRef = db.collection("hotels").document(hotelId);
                transaction.update(hotelRef, "bookedRooms", FieldValue.increment((Integer) bookingDetails.get("roomsBooked")));
                transaction.update(hotelRef, "status", "committed");
            }

            return "Travel Package " + packageId + " confirmed successfully.";
        });
    }

    // 3. Abort Phase of the 2PC Booking (Cancel Booking)
    public ApiFuture<String> cancelTravelPackage(String userId, String packageId) {
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
            String flightMessageId = brokerService.publishMessage("flight-cancel-request", packageSnapshot.getData());
            if (flightMessageId == null) {
                logger.error("Failed to publish flight-cancel-requests message");
            }
            for (Map<String, Object> flight : flights) {
                String flightId = (String) flight.get("flightId");
                DocumentReference flightRef = db.collection("flights").document(flightId);
                transaction.update(flightRef, "status", "cancel");
            }
            String hotelMessageId = brokerService.publishMessage("hotel-cancel-request", packageSnapshot.getData());
            if (hotelMessageId == null) {
                logger.error("Failed to publish flight-cancel-requests message");
            }
            for (Map<String, Object> hotel : hotels) {
                String hotelId = (String) hotel.get("hotelId");
                DocumentReference hotelRef = db.collection("hotels").document(hotelId);
                transaction.update(hotelRef, "bookedRooms", FieldValue.increment(-(Integer) hotel.get("roomsBooked")));
                transaction.update(hotelRef, "status", "cancel");
            }
            return "Travel Package " + packageId + " cancelled successfully.";
        });
    }

    // Methods for Before Booking
    public void addFlightToPackage(String userId, String packageId, Map<String, Object> flightDetails) throws ExecutionException, InterruptedException {
       Firestore db = firestore;
       db.runTransaction(transaction -> {
          DocumentReference packageRef = db.collection("travelPackages").document(packageId);
          transaction.update(packageRef, "flights", FieldValue.arrayUnion(flightDetails));
              return null;
       }).get();
    }

    public void removeFlightFromPackage(String userId, String packageId, String flightId) throws ExecutionException, InterruptedException {
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

    public void addHotelToPackage(String userId, String packageId, Map<String, Object> hotelDetails) throws ExecutionException, InterruptedException {
        Firestore db = firestore;
        db.runTransaction(transaction -> {
            DocumentReference packageRef = db.collection("travelPackages").document(packageId);
            transaction.update(packageRef, "hotels", FieldValue.arrayUnion(hotelDetails));
            return null;
        }).get();
    }

    public void removeHotelFromPackage(String userId, String packageId, String hotelId) throws ExecutionException, InterruptedException {
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

    public void addCustomerToPackage(String userId, String packageId, Map<String, Object> customerDetails) throws ExecutionException, InterruptedException {
        Firestore db = firestore;
        db.runTransaction(transaction -> {
            DocumentReference packageRef = db.collection("travelPackages").document(packageId);
            transaction.update(packageRef, "customers", FieldValue.arrayUnion(customerDetails));
            return null;
        }).get();
    }

    public void removeCustomerFromPackage(String userId, String packageId, String customerId) throws ExecutionException, InterruptedException {
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
    public void updateFlightInPackage(String userId, String packageId, Map<String, Object> flightDetails) throws ExecutionException, InterruptedException, IOException {
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

    public void updateHotelInPackage(String userId, String packageId, Map<String, Object> hotelDetails) throws ExecutionException, InterruptedException, IOException {
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

    public void updateCustomerInPackage(String userId, String packageId, Map<String, Object> customerDetails) {
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
