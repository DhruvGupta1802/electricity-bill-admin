package com.app;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.cloud.FirestoreClient;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

@Service
public class FirebaseService {

    private Firestore firestore;

    @PostConstruct
    public void init() {
        try {
            // Load credentials from environment variable
            String firebaseJson = System.getenv("FIREBASE_CREDENTIALS");

            if (firebaseJson == null) {
                throw new RuntimeException("FIREBASE_CREDENTIALS environment variable is missing!");
            }

            GoogleCredentials credentials = GoogleCredentials.fromStream(
                    new ByteArrayInputStream(firebaseJson.getBytes(StandardCharsets.UTF_8))
            );

            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(credentials)
                    .build();

            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
            }

            this.firestore = FirestoreClient.getFirestore();

            System.out.println("🔥 Firebase initialized successfully!");

        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize Firebase: " + e.getMessage(), e);
        }
    }

    public Firestore getFirestore() {
        return firestore;
    }
}
