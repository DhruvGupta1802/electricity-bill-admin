package com.app.service;

import com.app.model.Bill;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.FirestoreClient;
import com.google.firebase.messaging.*;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ExecutionException;

@Service
public class FirebaseService {

    private Firestore firestore;

    @PostConstruct
    public void init() {
        try {
            String creds = System.getenv("FIREBASE_CREDENTIALS");
            if (creds == null) {
                throw new RuntimeException("🚨 FIREBASE_CREDENTIALS env variable is missing!");
            }

            GoogleCredentials credentials = GoogleCredentials.fromStream(
                    new ByteArrayInputStream(creds.getBytes(StandardCharsets.UTF_8))
            );

            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(credentials)
                    .build();

            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
            }

            firestore = FirestoreClient.getFirestore();
            System.out.println("🔥 Firebase initialized successfully!");

        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize Firebase: " + e.getMessage(), e);
        }
    }

    // Save bill
    public String addBill(Bill bill) throws ExecutionException, InterruptedException {
        DocumentReference docRef = firestore.collection("bills").document();
        docRef.set(bill.toMap()).get();
        return docRef.getId();
    }

    // Fetch all FCM tokens
    public List<String> getAllFCMTokens() {
        List<String> tokens = new ArrayList<>();

        try {
            List<QueryDocumentSnapshot> documents = firestore.collection("fcmTokens").get().get().getDocuments();

            for (QueryDocumentSnapshot d : documents) {
                String token = d.getString("token");
                if (token != null && !token.isBlank()) tokens.add(token);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return tokens;
    }

    // Send push notification to all users
    public void sendNotificationToAll(String title, String body, String month) {
        List<String> tokens = getAllFCMTokens();
        if (tokens.isEmpty()) {
            System.out.println("⚠ No user tokens found.");
            return;
        }

        int batchSize = 500;
        for (int i = 0; i < tokens.size(); i += batchSize) {
            try {
                sendMulticastMessage(tokens.subList(i, Math.min(i + batchSize, tokens.size())),
                        title, body, month);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void sendMulticastMessage(List<String> tokens, String title, String body, String month)
            throws FirebaseMessagingException {

        Notification notification = Notification.builder()
                .setTitle(title)
                .setBody(body)
                .build();

        WebpushConfig webpushConfig = WebpushConfig.builder()
                .setNotification(WebpushNotification.builder()
                        .setTitle(title)
                        .setBody(body)
                        .setIcon("/icons/Icon-192.png")
                        .build())
                .build();

        MulticastMessage message = MulticastMessage.builder()
                .setNotification(notification)
                .setWebpushConfig(webpushConfig)
                .putData("month", month)
                .putData("type", "new_bill")
                .addAllTokens(tokens)
                .build();

        BatchResponse response = FirebaseMessaging.getInstance().sendEachForMulticast(message);

        System.out.println("📤 Success: " + response.getSuccessCount() +
                " | Failed: " + response.getFailureCount());
    }

    public Firestore getFirestore() {
        return firestore;
    }
}
