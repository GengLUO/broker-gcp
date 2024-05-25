package be.kuleuven.dsgt4;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import be.kuleuven.dsgt4.auth.WebSecurityConfig;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.WriteResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AuthorizationServiceException;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

// Add the controller.
@RestController
@RequestMapping("/api")
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

        UUID buuid = UUID.randomUUID();
        UserMessage b = new UserMessage(buuid, LocalDateTime.now(), user.getRole(), user.getEmail());
        this.firestore.collection("usermessages").document(b.getId().toString()).set(b.toDoc()).get();

        return user;
    }

    // Add a document to Firestore
    @PostMapping("/add")
    public String addDocument(@RequestBody Map<String, Object> data) throws ExecutionException, InterruptedException {
        DocumentReference docRef = firestore.collection("testCollection").document();
        WriteResult result = docRef.set(data).get();
        return "Document added with ID: " + docRef.getId() + " at time: " + result.getUpdateTime();
    }

    // Retrieve a document from Firestore
    @GetMapping("/get/{id}")
    public Map<String, Object> getDocument(@PathVariable String id) throws ExecutionException, InterruptedException {
        DocumentReference docRef = firestore.collection("testCollection").document(id);
        DocumentSnapshot document = docRef.get().get();

        if (document.exists()) {
            return document.getData();
        } else {
            throw new RuntimeException("Document with ID " + id + " not found");
        }
    }

    // Update a document in Firestore
    @PutMapping("/update/{id}")
    public String updateDocument(@PathVariable String id, @RequestBody Map<String, Object> data) throws ExecutionException, InterruptedException {
        DocumentReference docRef = firestore.collection("testCollection").document(id);
        WriteResult result = docRef.update(data).get();
        return "Document with ID " + id + " updated at time: " + result.getUpdateTime();
    }

    // Delete a document from Firestore
    @DeleteMapping("/delete/{id}")
    public String deleteDocument(@PathVariable String id) throws ExecutionException, InterruptedException {
        DocumentReference docRef = firestore.collection("testCollection").document(id);
        WriteResult result = docRef.delete().get();
        return "Document with ID " + id + " deleted at time: " + result.getUpdateTime();
    }
}
