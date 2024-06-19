package be.kuleuven.dsgt4.broker.services;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import com.google.cloud.firestore.v1.FirestoreClient;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@Service
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(TransactionCoordinatorService.class);

    @Autowired
    private Firestore firestore;

    public void createUser(String userId, Map<String, Object> userDetails) throws ExecutionException, InterruptedException {
        DocumentReference docRef = firestore.collection("users").document(userId);
        ApiFuture<WriteResult> result = docRef.set(userDetails);
        result.get();
    }

    public Map<String, Object> getUser(String userId) throws ExecutionException, InterruptedException {
        DocumentReference docRef = firestore.collection("users").document(userId);
        ApiFuture<DocumentSnapshot> future = docRef.get();
        DocumentSnapshot document = future.get();
        if (document.exists()) {
            return document.getData();
        } else {
            throw new IllegalArgumentException("User not found");
        }
    }

    public void updateUser(String userId, Map<String, Object> userDetails) throws ExecutionException, InterruptedException {
        DocumentReference docRef = firestore.collection("users").document(userId);
        ApiFuture<WriteResult> result = docRef.update(userDetails);
        result.get();
    }

    public void deleteUser(String userId) throws ExecutionException, InterruptedException {
        DocumentReference docRef = firestore.collection("users").document(userId);
        ApiFuture<WriteResult> result = docRef.delete();
        result.get();
    }

    public List<Map<String, Object>> getAllCustomers() throws InterruptedException, ExecutionException {
        ApiFuture<QuerySnapshot> future = firestore.collection("users").get();
        List<QueryDocumentSnapshot> documents = future.get().getDocuments();
        List<Map<String, Object>> users = new ArrayList<>();
        for (QueryDocumentSnapshot document : documents) {
            logger.info("User Document ID: " + document.getId() + " => " + document.getData());
            users.add(document.getData());
        }
        return users;
    }

    public List<Map<String, Object>> getAllOrders() throws InterruptedException, ExecutionException {
        ApiFuture<QuerySnapshot> future = firestore.collection("travelPackages").get();
        List<QueryDocumentSnapshot> documents = future.get().getDocuments();
        List<Map<String, Object>> orders = new ArrayList<>();
        for (QueryDocumentSnapshot document : documents) {
            logger.info("Travel packages Document ID: " + document.getId() + " => " + document.getData());
            orders.add(document.getData());
        }
        return orders;
    }

    public List<Map<String, Object>> getUserBookings(String userId) throws ExecutionException, InterruptedException {
        // Query the travelPackages collection for all documents where the userId field is equal to the userId parameter
        ApiFuture<QuerySnapshot> future = firestore.collection("travelPackages").whereEqualTo("userId", userId).get();
        List<QueryDocumentSnapshot> documents = future.get().getDocuments();
        List<Map<String, Object>> bookingDetails = new ArrayList<>();

        // Iterate over the documents and extract the relevant fields
        for (QueryDocumentSnapshot document : documents) {
            Map<String, Object> docData = document.getData();
            Map<String, Object> bookingDetail = new HashMap<>();
            
            bookingDetail.put("packageId", docData.get("packageId"));
            bookingDetail.put("customerName", getCustomerName(docData));
            bookingDetail.put("flightDestination", getFlightDetail(docData, "flightDestination"));
            bookingDetail.put("flightDate", getFlightDetail(docData, "flightDate"));
            bookingDetail.put("flightId", getFlightDetail(docData, "flightId"));
            bookingDetail.put("seatsBooked", getFlightDetail(docData, "seatsBooked"));
            bookingDetail.put("flightConfirmStatus", docData.getOrDefault("flightConfirmStatus", "pending"));
            bookingDetail.put("hotelDestination", getHotelDetail(docData, "hotelDestination"));
            bookingDetail.put("hotelDate", getHotelDetail(docData, "hotelDate"));
            bookingDetail.put("hotelDays", getHotelDetail(docData, "hotelDays"));
            bookingDetail.put("hotelId", getHotelDetail(docData, "hotelId"));
            bookingDetail.put("roomsBooked", getHotelDetail(docData, "roomsBooked"));
            bookingDetail.put("hotelConfirmStatus", docData.getOrDefault("hotelConfirmStatus", "pending"));

            bookingDetails.add(bookingDetail);
        }
        return bookingDetails;
    }

    private String getCustomerName(Map<String, Object> docData) {
        List<Map<String, Object>> flights = (List<Map<String, Object>>) docData.get("flights");
        if (flights != null && !flights.isEmpty()) {
            return (String) flights.get(0).get("customerName");
        }
        return "N/A";
    }

    private String getFlightDetail(Map<String, Object> docData, String detail) {
        List<Map<String, Object>> flights = (List<Map<String, Object>>) docData.get("flights");
        if (flights != null && !flights.isEmpty()) {
            return flights.get(0).get(detail) != null ? flights.get(0).get(detail).toString() : "N/A";
        }
        return "N/A";
    }

    private String getHotelDetail(Map<String, Object> docData, String detail) {
        List<Map<String, Object>> hotels = (List<Map<String, Object>>) docData.get("hotels");
        if (hotels != null && !hotels.isEmpty()) {
            return hotels.get(0).get(detail) != null ? hotels.get(0).get(detail).toString() : "N/A";
        }
        return "N/A";
    }
}