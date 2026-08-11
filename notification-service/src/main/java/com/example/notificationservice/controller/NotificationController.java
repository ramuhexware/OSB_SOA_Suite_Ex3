package com.example.notificationservice.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/notification")
@CrossOrigin(origins = "*")
public class NotificationController {

    @PostMapping
    public ResponseEntity<?> sendNotification(@RequestBody Map<String, Object> request) {
        Number loanId = (Number) request.get("loanId");
        String applicantName = (String) request.get("applicantName");
        String decision = (String) request.get("decision");

        if (loanId == null || applicantName == null || decision == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "loanId, applicantName, and decision are required"));
        }

        String msg = String.format("SMS/Email Notification sent to %s regarding Loan Application ID %s. Decision: %s.",
                applicantName, loanId, decision);

        System.out.println("[Notification Service] " + msg);

        return ResponseEntity.ok(Map.of(
            "loanId", loanId,
            "status", "SUCCESS",
            "message", "Notification dispatched successfully via UMS: " + msg
        ));
    }
}
