package be.kuleuven.dsgt4.broker.services;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ExecutionException;

@Service
public class UserService {

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
}
