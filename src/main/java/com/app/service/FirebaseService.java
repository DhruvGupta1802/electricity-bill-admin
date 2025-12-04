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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

@Service
public class FirebaseService {

    @Value("${firebase.credentials.path}")
    private String credentialsPath;

    private Firestore firestore;

    // Initialize Firebase when application starts
    @PostConstruct
    public void initialize() {
        try {
            // Load serviceAccount.json from resources folder
            InputStream serviceAccount = getClass()
                .getClassLoader()
                .getResourceAsStream(credentialsPath);

            if (serviceAccount == null) {
                throw new IOException("❌ Firebase credentials file not found: " + credentialsPath);
            }

            // Initialize Firebase
            FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                .build();

            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
            }

            firestore = FirestoreClient.getFirestore();
            System.out.println("✅ Firebase initialized successfully!");

        } catch (IOException e) {
            System.err.println("❌ Error initializing Firebase: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Add a new bill to Firestore
    public String addBill(Bill bill) throws ExecutionException, InterruptedException {
        DocumentReference docRef = firestore.collection("bills").document();
        docRef.set(bill.toMap()).get();
        
        System.out.println("✅ Bill added with ID: " + docRef.getId());
        return docRef.getId();
    }

    // Get all FCM tokens from Firestore
    public List<String> getAllFCMTokens() {
        List<String> tokens = new ArrayList<>();
        
        try {
            List<QueryDocumentSnapshot> documents = firestore
                .collection("fcmTokens")
                .get()
                .get()
                .getDocuments();

            for (QueryDocumentSnapshot document : documents) {
                String token = document.getString("token");
                if (token != null && !token.isEmpty()) {
                    tokens.add(token);
                }
            }
            
            System.out.println("✅ Retrieved " + tokens.size() + " FCM tokens");
            
        } catch (Exception e) {
            System.err.println("❌ Error fetching FCM tokens: " + e.getMessage());
            e.printStackTrace();
        }
        
        return tokens;
    }

    // Send push notification to all users
    public void sendNotificationToAll(String title, String body, String month) {
        List<String> tokens = getAllFCMTokens();
        
        if (tokens.isEmpty()) {
            System.out.println("⚠️ No FCM tokens found. Skipping notifications.");
            return;
        }

        // FCM allows max 500 tokens per request, so we split into batches
        int batchSize = 500;
        for (int i = 0; i < tokens.size(); i += batchSize) {
            int end = Math.min(i + batchSize, tokens.size());
            List<String> batchTokens = tokens.subList(i, end);
            
            try {
                sendMulticastMessage(batchTokens, title, body, month);
            } catch (Exception e) {
                System.err.println("❌ Error sending notifications to batch: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    // Send notification to a batch of tokens
    private void sendMulticastMessage(List<String> tokens, String title, String body, String month) 
        throws FirebaseMessagingException {
        
        // Create notification
        Notification notification = Notification.builder()
            .setTitle(title)
            .setBody(body)
            .build();

        // Web-specific configuration
        WebpushConfig webpushConfig = WebpushConfig.builder()
            .setNotification(WebpushNotification.builder()
                .setTitle(title)
                .setBody(body)
                .setIcon("/icons/Icon-192.png")
                .setTag("electricity-bill")
                .setRequireInteraction(false)
                .build())
            .build();

        // Build message for multiple devices
        MulticastMessage message = MulticastMessage.builder()
            .setNotification(notification)
            .setWebpushConfig(webpushConfig)
            .putData("month", month)
            .putData("type", "new_bill")
            .addAllTokens(tokens)
            .build();

        // Send message
        BatchResponse response = FirebaseMessaging.getInstance(). sendEachForMulticast(message);
        
        System.out.println("✅ Successfully sent " + response.getSuccessCount() + " messages");
        
        if (response.getFailureCount() > 0) {
            System.out.println("⚠️ Failed to send " + response.getFailureCount() + " messages");
            
            List<SendResponse> responses = response.getResponses();
            for (int i = 0; i < responses.size(); i++) {
                if (!responses.get(i).isSuccessful()) {
                    System.err.println("Failed token: " + tokens.get(i));
                    System.err.println("Error: " + responses.get(i).getException().getMessage());
                }
            }
        }
    }

    public Firestore getFirestore() {
        return firestore;
    }
}