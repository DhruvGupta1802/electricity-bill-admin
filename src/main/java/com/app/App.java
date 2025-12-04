package com.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class App {
    public static void main(String[] args) {
        SpringApplication.run(App.class, args);
        System.out.println("\n");
        System.out.println("═══════════════════════════════════════════════");
        System.out.println("✅ Admin Panel Started Successfully!");
        System.out.println("🌐 Open: http://localhost:8080");
        System.out.println("═══════════════════════════════════════════════");
        System.out.println("\n");
    }
}