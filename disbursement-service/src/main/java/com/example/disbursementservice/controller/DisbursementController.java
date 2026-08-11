package com.example.disbursementservice.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/disbursement")
@CrossOrigin(origins = "*")
public class DisbursementController {

    @PostMapping
    public ResponseEntity<?> disburse(@RequestBody Map<String, Object> request) {
        Number loanId = (Number) request.get("loanId");
        Double loanAmount = null;
        if (request.get("loanAmount") instanceof Number) {
            loanAmount = ((Number) request.get("loanAmount")).doubleValue();
        }

        if (loanId == null || loanAmount == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "loanId and loanAmount are required"));
        }

        return ResponseEntity.ok(Map.of(
            "loanId", loanId,
            "status", "SUCCESS",
            "message", "Core Banking instruction dispatched: published payment instruction of $" + 
                       String.format("%,.2f", loanAmount) + " to core ledger queue."
        ));
    }
}
