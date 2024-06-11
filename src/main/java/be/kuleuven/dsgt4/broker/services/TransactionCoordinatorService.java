package be.kuleuven.dsgt4.broker.services;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class TransactionCoordinatorService {

    private static final Logger logger = LoggerFactory.getLogger(TransactionCoordinatorService.class);

    @Autowired
    private Firestore firestore;

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

            // Here you should update Firestore with booking details under the user's document.
            // Assuming bookingDetails contains userId and other necessary details.
            String userId = (String) bookingDetails.get("userId");
            DocumentReference userRef = db.collection("users").document(userId).collection("bookings").document();
            transaction.set(userRef, bookingDetails);
            
            return "Travel Package " + packageId + " booked successfully.";
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
