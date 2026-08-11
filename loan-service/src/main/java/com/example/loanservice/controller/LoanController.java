package com.example.loanservice.controller;

import com.example.loanservice.model.AuditLogEntry;
import com.example.loanservice.model.LoanApplication;
import com.example.loanservice.repository.AuditLogRepository;
import com.example.loanservice.repository.LoanApplicationRepository;
import com.example.loanservice.service.OrchestrationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/loans")
@CrossOrigin(origins = "*")
public class LoanController {

    @Autowired
    private LoanApplicationRepository loanRepository;

    @Autowired
    private AuditLogRepository auditRepository;

    @Autowired
    private OrchestrationService orchestrationService;

    @PostMapping
    public ResponseEntity<?> submitLoan(@RequestBody LoanApplication loanApplication) {
        if (loanApplication.getApplicantName() == null || loanApplication.getApplicantName().trim().isEmpty() ||
            loanApplication.getSsn() == null || loanApplication.getSsn().trim().isEmpty() ||
            loanApplication.getLoanAmount() == null || loanApplication.getLoanAmount() <= 0 ||
            loanApplication.getMonthlyIncome() == null || loanApplication.getMonthlyIncome() <= 0 ||
            loanApplication.getPropertyValue() == null || loanApplication.getPropertyValue() <= 0) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid or missing fields in request body"));
        }
        try {
            LoanApplication savedLoan = orchestrationService.initiateLoanWorkflow(loanApplication);
            return ResponseEntity.ok(savedLoan);
        } catch (OrchestrationService.SanctionListException sle) {
            return ResponseEntity.status(403).body(Map.of("error", sle.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<List<LoanApplication>> getAllLoans() {
        return ResponseEntity.ok(loanRepository.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<LoanApplication> getLoan(@PathVariable Long id) {
        return loanRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/logs")
    public ResponseEntity<List<AuditLogEntry>> getLoanLogs(@PathVariable Long id) {
        if (!loanRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        List<AuditLogEntry> logs = auditRepository.findByLoanApplicationIdOrderByTimestampAsc(id);
        return ResponseEntity.ok(logs);
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<LoanApplication> approveLoan(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String comments = body != null ? body.getOrDefault("comments", "Approved manually") : "Approved manually";
        try {
            orchestrationService.approveManualTask(id, comments);
            return loanRepository.findById(id)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<LoanApplication> rejectLoan(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String comments = body != null ? body.getOrDefault("comments", "Rejected manually") : "Rejected manually";
        try {
            orchestrationService.rejectManualTask(id, comments);
            return loanRepository.findById(id)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<LoanApplication> cancelLoan(@PathVariable Long id) {
        try {
            orchestrationService.cancelLoanWorkflow(id);
            return loanRepository.findById(id)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(null);
        }
    }
}
