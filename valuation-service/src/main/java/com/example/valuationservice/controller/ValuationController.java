package com.example.valuationservice.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/valuation")
@CrossOrigin(origins = "*")
public class ValuationController {

    @PostMapping
    public ResponseEntity<?> getPropertyValue(@RequestBody Map<String, Object> request) {
        String address = (String) request.get("propertyAddress");
        Double estimatedValue = null;
        if (request.get("estimatedValue") instanceof Number) {
            estimatedValue = ((Number) request.get("estimatedValue")).doubleValue();
        }

        if (address == null || address.trim().isEmpty() || estimatedValue == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "propertyAddress and estimatedValue are required"));
        }

        // Return appraised value equal to estimated value (simulating a successful appraisal)
        // and acknowledge reservation of $500 appraisal fee
        return ResponseEntity.ok(Map.of(
            "propertyAddress", address,
            "appraisedValue", estimatedValue,
            "status", "SUCCESS",
            "message", "Property valuation complete. Fee of $500 reserved."
        ));
    }

    @PostMapping("/refund")
    public ResponseEntity<?> refundAppraisalFee(@RequestBody Map<String, Object> request) {
        String address = (String) request.get("propertyAddress");
        return ResponseEntity.ok(Map.of(
            "propertyAddress", address != null ? address : "Unknown",
            "status", "REFUNDED",
            "message", "Compensation Triggered: Refunding $500 property appraisal fee to applicant account."
        ));
    }
}
