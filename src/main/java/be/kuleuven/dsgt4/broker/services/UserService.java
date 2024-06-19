package be.kuleuven.dsgt4.broker.services;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import com.google.cloud.firestore.v1.FirestoreClient;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

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
        DocumentReference userRef = firestore.collection("users").document(userId);
        DocumentSnapshot userSnapshot = userRef.get().get();
        List<DocumentReference> travelPackages = (List<DocumentReference>) userSnapshot.get("travelPackages");
        if (travelPackages == null) {
            return Collections.emptyList();
        }
        List<Map<String, Object>> bookingDetails = new ArrayList<>();
        for (DocumentReference packageRef : travelPackages) {
            DocumentSnapshot packageSnapshot = packageRef.get().get();
            bookingDetails.add(packageSnapshot.getData());
        }
        return bookingDetails;
    }
}
