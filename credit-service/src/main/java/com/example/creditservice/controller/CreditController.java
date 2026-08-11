package com.example.creditservice.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.Random;

@RestController
@RequestMapping("/api/credit")
@CrossOrigin(origins = "*")
public class CreditController {

    private final Random random = new Random();

    @GetMapping
    public ResponseEntity<?> getCreditScore(@RequestParam String ssn) {
        if (ssn == null || ssn.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "SSN is required"));
        }

        // Clean SSN
        String cleanSsn = ssn.replaceAll("\\D", "");

        // Simulate Connection Timeout/Latency for retry testing (SSN ending with 4444)
        if (cleanSsn.endsWith("4444")) {
            try {
                // Sleep for 6 seconds to trigger client timeout
                Thread.sleep(6000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return ResponseEntity.status(504).body(Map.of("error", "Gateway Timeout from Equifax bureau."));
        }

        int score = 650;
        if (cleanSsn.endsWith("1111")) {
            score = 780; // Good credit -> Auto Approval
        } else if (cleanSsn.endsWith("2222")) {
            score = 520; // Poor credit -> Auto Reject
        } else if (cleanSsn.endsWith("3333")) {
            score = 670; // Fair credit -> Manual Review
        } else {
            score = 550 + random.nextInt(251); // Random score between 550 and 800
        }

        return ResponseEntity.ok(Map.of(
            "ssn", ssn,
            "creditScore", score
        ));
    }
}
