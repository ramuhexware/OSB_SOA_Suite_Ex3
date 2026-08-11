package com.example.underwritingservice.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/underwriting")
@CrossOrigin(origins = "*")
public class UnderwritingController {

    public enum UnderwritingDecision {
        AUTO_APPROVED,
        AUTO_REJECTED,
        MANUAL_REVIEW
    }

    @PostMapping
    public ResponseEntity<?> evaluate(@RequestBody Map<String, Object> request) {
        Integer creditScore = (Integer) request.get("creditScore");
        Double loanAmount = null;
        Double monthlyIncome = null;
        Double propertyValue = null;

        if (request.get("loanAmount") instanceof Number) {
            loanAmount = ((Number) request.get("loanAmount")).doubleValue();
        }
        if (request.get("monthlyIncome") instanceof Number) {
            monthlyIncome = ((Number) request.get("monthlyIncome")).doubleValue();
        }
        if (request.get("propertyValue") instanceof Number) {
            propertyValue = ((Number) request.get("propertyValue")).doubleValue();
        }

        if (creditScore == null || loanAmount == null || monthlyIncome == null || propertyValue == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "creditScore, loanAmount, monthlyIncome, and propertyValue are required"));
        }

        // Calculate LTV
        double ltv = loanAmount / propertyValue;

        // Calculate DTI (monthly payment estimated at 0.6% of loan amount)
        double monthlyPayment = loanAmount * 0.006;
        double dti = monthlyPayment / monthlyIncome;

        UnderwritingDecision decision;
        String notes;

        // 1. Hard Rejection Rules
        if (creditScore < 600) {
            decision = UnderwritingDecision.AUTO_REJECTED;
            notes = "Credit score of " + creditScore + " is below minimum requirement of 600.";
        } else if (ltv > 0.95) {
            decision = UnderwritingDecision.AUTO_REJECTED;
            notes = "Loan-to-Value (LTV) ratio of " + String.format("%.2f%%", ltv * 100) + " exceeds maximum threshold of 95.00%.";
        } else if (dti > 0.45) {
            decision = UnderwritingDecision.AUTO_REJECTED;
            notes = "Debt-to-Income (DTI) ratio of " + String.format("%.2f%%", dti * 100) + " exceeds maximum threshold of 45.00%.";
        }
        // 2. Automated Approval Rules
        else if (creditScore >= 740 && dti <= 0.36 && ltv <= 0.80) {
            decision = UnderwritingDecision.AUTO_APPROVED;
            notes = "Auto-Approved: Credit Score >= 740, DTI <= 36.00%, and LTV <= 80.00%. Excellent profile.";
        }
        // 3. Fallback to Manual Underwriting
        else {
            decision = UnderwritingDecision.MANUAL_REVIEW;
            StringBuilder sb = new StringBuilder("Manual Review Required: ");
            if (creditScore < 740) {
                sb.append("Credit score (").append(creditScore).append(") is moderate (< 740). ");
            }
            if (dti > 0.36) {
                sb.append("DTI ratio (").append(String.format("%.2f%%", dti * 100)).append(") is elevated (> 36.00%). ");
            }
            if (ltv > 0.80) {
                sb.append("LTV ratio (").append(String.format("%.2f%%", ltv * 100)).append(") is elevated (> 80.00%). ");
            }
            notes = sb.toString().trim();
        }

        Map<String, Object> response = new HashMap<>();
        response.put("decision", decision.name());
        response.put("notes", notes);
        response.put("dti", dti);
        response.put("ltv", ltv);

        return ResponseEntity.ok(response);
    }
}
