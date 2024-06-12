package be.kuleuven.dsgt4.broker.services;

import com.google.api.core.ApiFuture;
import com.google.api.core.ApiFutures;
import com.google.api.core.ApiFutureCallback;
import com.google.cloud.firestore.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.gson.Gson;

import java.util.Map;
import java.util.List;
import java.util.concurrent.ExecutionException;

import be.kuleuven.dsgt4.broker.domain.BookingTransaction;

@Service
public class TransactionCoordinatorService {

    private static final Logger logger = LoggerFactory.getLogger(TransactionCoordinatorService.class);

    @Autowired
    private Firestore firestore;

    @Autowired
    private PBFTService pbftService;

    private final Gson gson = new Gson();

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

            List<Map<String, Object>> flights = (List<Map<String, Object>>) packageSnapshot.get("flights");
            List<Map<String, Object>> hotels = (List<Map<String, Object>>) packageSnapshot.get("hotels");

            if (!pbftService.initiateConsensus(packageId)) {
                throw new IllegalStateException("PBFT consensus failed for package ID: " + packageId);
            }

            for (Map<String, Object> flight : flights) {
                String flightId = (String) flight.get("flightId");
                DocumentReference flightRef = db.collection("flights").document(flightId);
                transaction.update(flightRef, "booked", true);
            }

            for (Map<String, Object> hotel : hotels) {
                String hotelId = (String) hotel.get("hotelId");
                DocumentReference hotelRef = db.collection("hotels").document(hotelId);
                transaction.update(hotelRef, "bookedRooms", FieldValue.increment((Integer) bookingDetails.get("roomsBooked")));
            }

            String userId = (String) bookingDetails.get("userId");
            DocumentReference userRef = db.collection("users").document(userId).collection("bookings").document(packageId);
            transaction.set(userRef, bookingDetails);

            return "Travel Package " + packageId + " booked successfully.";
        });
    }

    public void processBookingResponse(String message) {
        Map<String, Object> bookingResponse = parseMessage(message);
        String transactionId = (String) bookingResponse.get("transactionId");
        boolean success = (boolean) bookingResponse.get("success");
        ApiFuture<DocumentSnapshot> future = firestore.collection("transactions").document(transactionId).get();
        try {
            DocumentSnapshot document = future.get();
            if (document.exists()) {
                BookingTransaction bookingTransaction = document.toObject(BookingTransaction.class);

                if (success) {
                    if (pbftService.initiateConsensus(transactionId)) {
                        commitTransaction(bookingTransaction);
                    } else {
                        abortTransaction(bookingTransaction);
                    }
                } else {
                    abortTransaction(bookingTransaction);
                }
            } else {
                logger.error("Transaction not found for ID: " + transactionId);
            }
        } catch (InterruptedException | ExecutionException e) {
            logger.error("Error processing booking response: ", e);
        }
    }

    private Map<String, Object> parseMessage(String message) {
        return gson.fromJson(message, Map.class);
    }

    private void commitTransaction(BookingTransaction bookingTransaction) {
        Firestore db = firestore;
        ApiFuture<Void> future = db.runTransaction(transaction -> {
            for (String flightJson : bookingTransaction.getFlightIds()) {
                Map<String, Object> flightMap = parseJson(flightJson);
                String flightId = (String) flightMap.get("flightId");
                DocumentReference flightRef = db.collection("flights").document(flightId);
                transaction.update(flightRef, "status", "confirmed");
            }

            for (String hotelJson : bookingTransaction.getHotelIds()) {
                Map<String, Object> hotelMap = parseJson(hotelJson);
                String hotelId = (String) hotelMap.get("hotelId");
                DocumentReference hotelRef = db.collection("hotels").document(hotelId);
                transaction.update(hotelRef, "status", "confirmed");
            }

            DocumentReference userRef = db.collection("users").document(bookingTransaction.getUserId()).collection("bookings").document(bookingTransaction.getTransactionId());
            transaction.update(userRef, "status", "confirmed");

            return null;
        });

        ApiFutures.addCallback(future, new ApiFutureCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                logger.info("Transaction committed successfully");
            }

            @Override
            public void onFailure(Throwable t) {
                logger.error("Transaction commit failed", t);
            }
        }, Runnable::run);
    }

    private void abortTransaction(BookingTransaction bookingTransaction) {
        Firestore db = firestore;
        ApiFuture<Void> future = db.runTransaction(transaction -> {
            for (String flightJson : bookingTransaction.getFlightIds()) {
                Map<String, Object> flightMap = parseJson(flightJson);
                String flightId = (String) flightMap.get("flightId");
                DocumentReference flightRef = db.collection("flights").document(flightId);
                transaction.update(flightRef, "status", "available");
            }

            for (String hotelJson : bookingTransaction.getHotelIds()) {
                Map<String, Object> hotelMap = parseJson(hotelJson);
                String hotelId = (String) hotelMap.get("hotelId");
                DocumentReference hotelRef = db.collection("hotels").document(hotelId);
                transaction.update(hotelRef, "status", "available");
            }

            DocumentReference userRef = db.collection("users").document(bookingTransaction.getUserId()).collection("bookings").document(bookingTransaction.getTransactionId());
            transaction.update(userRef, "status", "failed");

            return null;
        });

        ApiFutures.addCallback(future, new ApiFutureCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                logger.info("Transaction aborted successfully");
            }

            @Override
            public void onFailure(Throwable t) {
                logger.error("Transaction abort failed", t);
            }
        }, Runnable::run);
    }

    private Map<String, Object> parseJson(String json) {
        return gson.fromJson(json, Map.class);
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

            transaction.delete(docRef);
            return null;
        });
    }
}
