package com.example.loanservice.model;

public enum LoanStatus {
    SUBMITTED,
    CREDIT_CHECK_COMPLETED,
    VALUATION_COMPLETED,
    UNDERWRITING_COMPLETED,
    PENDING_MANUAL_REVIEW,
    APPROVED,
    REJECTED,
    DISBURSED,
    CANCELLED
}
