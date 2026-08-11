package com.example.loanservice.model;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.time.LocalDateTime;

@Entity
public class LoanApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String applicantName;
    private String ssn;
    private Double loanAmount;
    private Double monthlyIncome;
    private String propertyAddress;
    private Double propertyValue;

    // Computed / Retrieved fields during the workflow
    private Integer creditScore;
    private Double dtiRatio; // Debt-to-Income
    private Double ltvRatio; // Loan-to-Value

    @Enumerated(EnumType.STRING)
    private LoanStatus status;

    private String decision; // APPROVED, REJECTED, PENDING
    private String decisionNotes;
    private LocalDateTime submissionDate;

    public LoanApplication() {
        this.submissionDate = LocalDateTime.now();
        this.status = LoanStatus.SUBMITTED;
        this.decision = "PENDING";
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getApplicantName() { return applicantName; }
    public void setApplicantName(String applicantName) { this.applicantName = applicantName; }

    public String getSsn() { return ssn; }
    public void setSsn(String ssn) { this.ssn = ssn; }

    public Double getLoanAmount() { return loanAmount; }
    public void setLoanAmount(Double loanAmount) { this.loanAmount = loanAmount; }

    public Double getMonthlyIncome() { return monthlyIncome; }
    public void setMonthlyIncome(Double monthlyIncome) { this.monthlyIncome = monthlyIncome; }

    public String getPropertyAddress() { return propertyAddress; }
    public void setPropertyAddress(String propertyAddress) { this.propertyAddress = propertyAddress; }

    public Double getPropertyValue() { return propertyValue; }
    public void setPropertyValue(Double propertyValue) { this.propertyValue = propertyValue; }

    public Integer getCreditScore() { return creditScore; }
    public void setCreditScore(Integer creditScore) { this.creditScore = creditScore; }

    public Double getDtiRatio() { return dtiRatio; }
    public void setDtiRatio(Double dtiRatio) { this.dtiRatio = dtiRatio; }

    public Double getLtvRatio() { return ltvRatio; }
    public void setLtvRatio(Double ltvRatio) { this.ltvRatio = ltvRatio; }

    public LoanStatus getStatus() { return status; }
    public void setStatus(LoanStatus status) { this.status = status; }

    public String getDecision() { return decision; }
    public void setDecision(String decision) { this.decision = decision; }

    public String getDecisionNotes() { return decisionNotes; }
    public void setDecisionNotes(String decisionNotes) { this.decisionNotes = decisionNotes; }

    public LocalDateTime getSubmissionDate() { return submissionDate; }
    public void setSubmissionDate(LocalDateTime submissionDate) { this.submissionDate = submissionDate; }
}
