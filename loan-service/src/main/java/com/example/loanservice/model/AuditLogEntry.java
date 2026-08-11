package com.example.loanservice.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import java.time.LocalDateTime;

@Entity
public class AuditLogEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long loanApplicationId;
    private String stageName;

    @Lob
    private String springBootDetail;

    @Lob
    private String osbSoaSuiteMapping;

    private String status; // SUCCESS, PENDING, REJECTED, INFO, WARNING, COMPENSATED, FAILED
    private LocalDateTime timestamp;

    public AuditLogEntry() {}

    public AuditLogEntry(Long loanApplicationId, String stageName, String springBootDetail, String osbSoaSuiteMapping, String status) {
        this.loanApplicationId = loanApplicationId;
        this.stageName = stageName;
        this.springBootDetail = springBootDetail;
        this.osbSoaSuiteMapping = osbSoaSuiteMapping;
        this.status = status;
        this.timestamp = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getLoanApplicationId() { return loanApplicationId; }
    public void setLoanApplicationId(Long loanApplicationId) { this.loanApplicationId = loanApplicationId; }

    public String getStageName() { return stageName; }
    public void setStageName(String stageName) { this.stageName = stageName; }

    public String getSpringBootDetail() { return springBootDetail; }
    public void setSpringBootDetail(String springBootDetail) { this.springBootDetail = springBootDetail; }

    public String getOsbSoaSuiteMapping() { return osbSoaSuiteMapping; }
    public void setOsbSoaSuiteMapping(String osbSoaSuiteMapping) { this.osbSoaSuiteMapping = osbSoaSuiteMapping; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}
