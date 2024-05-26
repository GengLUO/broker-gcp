package be.kuleuven.dsgt4.auth;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

//reference: https://console.firebase.google.com/u/1/project/broker-da44b/settings/serviceaccounts/adminsdk
@Component
public class FirebaseInitializer {

    private static final String SERVICE_ACCOUNT_KEY_PATH = "src/main/java/be/kuleuven/dsgt4/auth/firebase-adminsdk.json";
    private static final String PROJECT_ID = "broker-da44b";
//    TODO: put them in env variable
//    @Value("${firebase.service-account-key-path}")
//    private String serviceAccountKeyPath;
//
//    @Value("${firebase.project-id}")
//    private String projectId;

    @PostConstruct
    public void initialize() {
        try {
            FileInputStream serviceAccount = new FileInputStream(SERVICE_ACCOUNT_KEY_PATH);

            FirebaseOptions options = new FirebaseOptions.Builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .setProjectId(PROJECT_ID)
                    .build();

            FirebaseApp.initializeApp(options);
            System.out.println("Finish Firebase initializer");
            setManagerRole("lOF97vEdM3Mxyma9w39MTyGSTZE2");
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to initialize Firebase", e);
        }
    }

    public void setCustomClaims(String uid, Map<String, Object> claims) {
        try {
            FirebaseAuth.getInstance().setCustomUserClaims(uid, claims);
            System.out.println("Custom claims set for user: " + uid);
        } catch (FirebaseAuthException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to set custom claims", e);
        }
    }

    public void setManagerRole(String uid) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("roles", new String[]{"manager"});
        setCustomClaims(uid, claims);
    }
}