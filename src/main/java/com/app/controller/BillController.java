package com.app.controller;

import com.app.model.Bill;
import com.app.service.FirebaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Date;

@Controller
public class BillController {

    @Autowired
    private FirebaseService firebaseService;

    // Show the form page
    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("pageTitle", "Add Electricity Bill");
        return "addBill";
    }

    // Handle form submission
    @PostMapping("/addBill")
    public String addBill(
            @RequestParam("month") String month,
            @RequestParam("amount") Double amount,
            @RequestParam("units") Integer units,
            @RequestParam("dueDate") @DateTimeFormat(pattern = "yyyy-MM-dd") Date dueDate,
            @RequestParam(value = "notes", required = false, defaultValue = "") String notes,
            RedirectAttributes redirectAttributes) {

        try {
            // Create bill object
            Bill bill = Bill.builder()
                .month(month)
                .amount(amount)
                .units(units)
                .dueDate(dueDate)
                .notes(notes)
                .createdAt(new Date())
                .addedBy("Admin")
                .build();

            // Save to Firestore
            String billId = firebaseService.addBill(bill);
            
            // Send push notifications to all users
            String notificationTitle = "New Electricity Bill Added";
            String notificationBody = String.format(
                "Your bill for %s is ready. Amount: ₹%.2f", 
                month, 
                amount
            );
            
            firebaseService.sendNotificationToAll(notificationTitle, notificationBody, month);

            // Show success message
            redirectAttributes.addFlashAttribute("successMessage", 
                "✅ Bill added successfully! ID: " + billId);
            redirectAttributes.addFlashAttribute("messageType", "success");

        } catch (Exception e) {
            // Show error message
            redirectAttributes.addFlashAttribute("errorMessage", 
                "❌ Error adding bill: " + e.getMessage());
            redirectAttributes.addFlashAttribute("messageType", "error");
            e.printStackTrace();
        }

        return "redirect:/";
    }
}