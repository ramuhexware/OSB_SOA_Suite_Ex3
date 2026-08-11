package com.example.loanservice.service;

import com.example.loanservice.model.AuditLogEntry;
import com.example.loanservice.model.LoanApplication;
import com.example.loanservice.model.LoanStatus;
import com.example.loanservice.repository.AuditLogRepository;
import com.example.loanservice.repository.LoanApplicationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class OrchestrationService {

    public static class SanctionListException extends RuntimeException {
        public SanctionListException(String message) { super(message); }
    }
    public static class FraudAlertException extends RuntimeException {
        public FraudAlertException(String message) { super(message); }
    }
    public static class CreditServiceDownFault extends RuntimeException {
        public CreditServiceDownFault(String message) { super(message); }
    }

    @Autowired
    private LoanApplicationRepository loanRepository;

    @Autowired
    private AuditLogRepository auditRepository;

    @Autowired
    private RestTemplate restTemplate;

    // Service URLs configured from application.properties
    @Value("${service.credit.url}")
    private String creditServiceUrl;

    @Value("${service.valuation.url}")
    private String valuationServiceUrl;

    @Value("${service.underwriting.url}")
    private String underwritingServiceUrl;

    @Value("${service.disbursement.url}")
    private String disbursementServiceUrl;

    @Value("${service.notification.url}")
    private String notificationServiceUrl;

    // Executor service to process long running workflows asynchronously, mimicking BPEL processes
    private final ExecutorService executorService = Executors.newCachedThreadPool();

    public LoanApplication initiateLoanWorkflow(LoanApplication loan) {
        // Sanction List Check (Exit condition check)
        if (loan.getApplicantName().toLowerCase().contains("voldemort") || "000-00-6666".equals(loan.getSsn())) {
            throw new SanctionListException("Security Exit: Applicant is on the Office of Foreign Assets Control (OFAC) sanction list.");
        }

        // Save initial state
        loan = loanRepository.save(loan);

        // Log Step 1: Submit Application
        addLog(loan.getId(),
            "Submit Application Request",
            "LoanApplication is received by LoanController, validated, and stored in H2 database. Initial state set to SUBMITTED.",
            "Oracle Service Bus (OSB) exposes a REST/SOAP Proxy Service (e.g., LoanServiceProxy) at the edge of the enterprise. " +
            "It validates the incoming XML schema, performs WS-Security verification, and routes the message to the BPEL process instance " +
            "LoanApprovalProcess using a SOAP adapter invocation. The BPEL process starts with a <receive> activity.",
            "SUCCESS"
        );

        // Run the workflow steps asynchronously so that it executes in the background
        final Long loanId = loan.getId();
        executorService.submit(() -> {
            try {
                processLoanWorkflow(loanId);
            } catch (Exception e) {
                System.err.println("Error processing workflow for loan ID " + loanId + ": " + e.getMessage());
            }
        });

        return loan;
    }

    public void processLoanWorkflow(Long loanId) {
        LoanApplication loan = loanRepository.findById(loanId).orElseThrow(() -> new IllegalArgumentException("Loan not found"));

        try {
            // Step 2: Credit Check (With retry logic simulation)
            loan.setStatus(LoanStatus.CREDIT_CHECK_COMPLETED);
            int creditScore = 0;

            String cleanSsn = loan.getSsn().replaceAll("\\D", "");

            if (cleanSsn.endsWith("4444")) {
                int maxRetries = 3;
                int attempt = 0;
                boolean success = false;
                while (attempt < maxRetries && !success) {
                    attempt++;
                    addLog(loanId,
                        "Query Credit Bureau (Attempt " + attempt + ")",
                        "Attempting connection to Equifax bureau REST endpoint. Response: Connection Timeout.",
                        "In BPEL, a <while> loop is configured with a condition matching a system fault variable. " +
                        "The loop contains a <wait> activity (configured for 500ms backoff) and an <invoke> activity to retry the credit check service.",
                        "WARNING"
                    );
                    try {
                        // RestTemplate call that will timeout
                        restTemplate.getForObject(creditServiceUrl + "?ssn=" + loan.getSsn(), Map.class);
                        success = true;
                    } catch (ResourceAccessException rae) {
                        // Expected timeout
                        try {
                            Thread.sleep(500); // Simulate <wait> delay
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                        }
                    } catch (Exception e) {
                        // Other error
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                        }
                    }
                }

                if (!success) {
                    addLog(loanId,
                        "Credit Check Multi-Bureau Aggregate",
                        "Failed to query Equifax after " + maxRetries + " attempts. Experian and TransUnion scores unavailable due to transaction grouping failure.",
                        "The BPEL <forEach> is configured with parallel=yes and a completionCondition. " +
                        "Since Equifax failed critically, the local <scope> fault handler catches the exception and re-throws a CreditServiceDownFault to the main process scope.",
                        "FAILED"
                    );
                    throw new CreditServiceDownFault("Equifax Credit Bureau is offline. Credit score aggregation aborted.");
                }
            } else {
                // Call credit service microservice
                try {
                    addLog(loanId,
                        "Query Credit Bureau (Equifax)",
                        "Initiating REST request to credit-service for Equifax bureau validation.",
                        "BPEL <flow> launches concurrent branches to query Equifax, Experian, and TransUnion in parallel.",
                        "SUCCESS"
                    );

                    Map<String, Object> response = restTemplate.getForObject(creditServiceUrl + "?ssn=" + loan.getSsn(), Map.class);
                    if (response != null && response.containsKey("creditScore")) {
                        creditScore = (Integer) response.get("creditScore");
                    } else {
                        creditScore = 650;
                    }
                } catch (Exception e) {
                    throw new CreditServiceDownFault("Credit bureau query failed: " + e.getMessage());
                }
            }

            loan.setCreditScore(creditScore);
            loan = loanRepository.save(loan);

            addLog(loanId,
                "Credit Check Integration",
                "Invoked credit-service microservice. Retrieved credit score: " + creditScore + ".",
                "In SOA Suite, BPEL performs an <invoke> activity to call the CreditCheckProxy on OSB. " +
                "OSB acts as a virtualization layer, transforming the BPEL request payload using XQuery/XSLT into a JSON payload. " +
                "It then routes to an external Credit Bureau REST Service via an HTTP Business Service, and transforms the response back to XML.",
                "SUCCESS"
            );

            // Step 3: Property Valuation
            loan.setStatus(LoanStatus.VALUATION_COMPLETED);

            double valuation = loan.getPropertyValue();
            try {
                Map<String, Object> request = new HashMap<>();
                request.put("propertyAddress", loan.getPropertyAddress());
                request.put("estimatedValue", loan.getPropertyValue());

                Map<String, Object> response = restTemplate.postForObject(valuationServiceUrl, request, Map.class);
                if (response != null && response.containsKey("appraisedValue")) {
                    valuation = ((Number) response.get("appraisedValue")).doubleValue();
                }
            } catch (Exception e) {
                // Fallback to estimated
                valuation = loan.getPropertyValue();
            }

            loan.setPropertyValue(valuation);
            loan = loanRepository.save(loan);

            addLog(loanId,
                "Property Appraisal Integration",
                "Invoked valuation-service microservice for address: " + loan.getPropertyAddress() +
                ". Appraisal value: $" + String.format("%,.2f", valuation) + ". Fee reservation of $500 confirmed.",
                "The BPEL process invokes the PropertyAppraisalProxy on OSB using a SOAP Web Service binding. " +
                "OSB handles the connection pooling, security, and failover routing to the property appraisal provider's endpoint.",
                "SUCCESS"
            );

            // Step 3.5: Fraud Check (Simulates saga compensation if security watchlist triggers)
            if (cleanSsn.endsWith("9999")) {
                throw new FraudAlertException("SSN matches national security watchlist for identity theft and financial fraud.");
            }

            // Step 4: Underwriting Decision
            loan.setStatus(LoanStatus.UNDERWRITING_COMPLETED);
            double dti = 0.0;
            double ltv = 0.0;
            String decisionStr = "MANUAL_REVIEW";
            String notes = "";

            try {
                Map<String, Object> request = new HashMap<>();
                request.put("creditScore", loan.getCreditScore());
                request.put("loanAmount", loan.getLoanAmount());
                request.put("monthlyIncome", loan.getMonthlyIncome());
                request.put("propertyValue", loan.getPropertyValue());

                Map<String, Object> response = restTemplate.postForObject(underwritingServiceUrl, request, Map.class);
                if (response != null) {
                    decisionStr = (String) response.get("decision");
                    notes = (String) response.get("notes");
                    dti = ((Number) response.get("dti")).doubleValue();
                    ltv = ((Number) response.get("ltv")).doubleValue();
                }
            } catch (Exception e) {
                notes = "Error connecting to underwriting-service rules engine. Forcing manual review: " + e.getMessage();
            }

            loan.setDtiRatio(dti);
            loan.setLtvRatio(ltv);
            loan.setDecisionNotes(notes);
            loan = loanRepository.save(loan);

            String statusStr = "SUCCESS";
            if ("AUTO_REJECTED".equals(decisionStr)) {
                statusStr = "REJECTED";
            }

            addLog(loanId,
                "Underwriting Rules Evaluation",
                "Invoked underwriting-service microservice. Calculated DTI: " + String.format("%.2f%%", dti * 100) +
                ", LTV: " + String.format("%.2f%%", ltv * 100) + ". Automated underwriting result: " + decisionStr + ".",
                "BPEL invokes an Oracle Business Rules (OBR) component inline. The OBR engine evaluates the loan facts against " +
                "declarative rules (e.g., CreditScore >= 600, DTI <= 45%, LTV <= 95%). " +
                "It returns a decision dictionary payload (e.g., AUTO_APPROVED, AUTO_REJECTED, MANUAL_REVIEW) back to BPEL.",
                statusStr
            );

            // Routing based on Rules Decision
            if ("AUTO_APPROVED".equals(decisionStr)) {
                approveLoan(loan);
            } else if ("AUTO_REJECTED".equals(decisionStr)) {
                rejectLoan(loan);
            } else {
                // Manual Review
                loan.setStatus(LoanStatus.PENDING_MANUAL_REVIEW);
                loan.setDecision("PENDING");
                loanRepository.save(loan);

                addLog(loanId,
                    "Initiate Human Workflow",
                    "Underwriting requires manual review. State updated to PENDING_MANUAL_REVIEW. Loan placed in Underwriter queue.",
                    "BPEL executes a <humanTask> activity. It deploys a workflow payload to the Human Task Service container. " +
                    "This creates a task instance in the SOA database, assigning it to the 'LoanOfficers' application role. " +
                    "The process halts at an asynchronous wait state, waiting for a completion callback from the BPM workflow engine.",
                    "PENDING"
                );
            }

        } catch (FraudAlertException fae) {
            addLog(loanId,
                "Fraud Alert Scope Fault Handler",
                "Caught FraudAlertException: " + fae.getMessage(),
                "The BPEL scope 'UnderwritingScope' catches the business fault FraudAlertFault using a specialized <catch> activity. " +
                "This halts further routing and triggers compensation logic for completed sibling scopes.",
                "FAILED"
            );

            // Execute Compensation Saga Rollback (Valuation Refund)
            try {
                Map<String, Object> request = new HashMap<>();
                request.put("propertyAddress", loan.getPropertyAddress());
                restTemplate.postForObject(valuationServiceUrl + "/refund", request, Map.class);
            } catch (Exception e) {
                System.err.println("Failed to trigger appraisal refund: " + e.getMessage());
            }

            addLog(loanId,
                "Appraisal Fee Compensation Rollback",
                "Compensation Triggered: Refunding $500 property appraisal fee to applicant account.",
                "The BPEL process invokes the <compensate> activity. This calls the <compensationHandler> registered in AppraisalScope, " +
                "invoking the PropertyAppraisalProxy SOAP operation 'refundReservation' to revert the appraisal fee transaction.",
                "COMPENSATED"
            );

            loan.setStatus(LoanStatus.REJECTED);
            loan.setDecision("REJECTED");
            loan.setDecisionNotes("Fraud Watchlist Alert triggered: " + fae.getMessage() + ". Appraisal fee has been compensated/refunded.");
            loanRepository.save(loan);

            sendNotification(loan);

        } catch (CreditServiceDownFault csdf) {
            addLog(loanId,
                "Credit Check Fault Handler",
                "System Fault: " + csdf.getMessage(),
                "The main BPEL process catches CreditServiceDownFault using a <catch> handler. It updates the status " +
                "and alerts operations using a UMS integration.",
                "FAILED"
            );

            loan.setStatus(LoanStatus.REJECTED);
            loan.setDecision("REJECTED");
            loan.setDecisionNotes("System Fault: Credit check service is offline. Please try again later.");
            loanRepository.save(loan);

            sendNotification(loan);

        } catch (Exception e) {
            addLog(loanId,
                "Workflow System Fault",
                "An unexpected exception occurred during orchestration: " + e.getMessage(),
                "In Oracle SOA Suite, BPEL traps this error using a <catchAll> block in Fault Handling. " +
                "This triggers a BPEL system fault, which can be monitored via Oracle Enterprise Manager (EM) console, " +
                "and halts the instance or triggers a rollback policy.",
                "FAILED"
            );
            loan.setStatus(LoanStatus.REJECTED);
            loan.setDecision("REJECTED");
            loan.setDecisionNotes("System Fault: " + e.getMessage());
            loanRepository.save(loan);
        }
    }

    public void approveManualTask(Long loanId, String comments) {
        LoanApplication loan = loanRepository.findById(loanId).orElseThrow(() -> new IllegalArgumentException("Loan not found"));
        if (loan.getStatus() != LoanStatus.PENDING_MANUAL_REVIEW) {
            throw new IllegalStateException("Loan is not in PENDING_MANUAL_REVIEW state");
        }

        addLog(loanId,
            "Complete Underwriter Human Task",
            "Loan Officer processed manual task and submitted approval. Comments: " + comments,
            "A loan officer logs into the BPM Worklist UI, claims the task, and clicks 'Approve'. " +
            "The Human Task Service completes the task, saves audit history, and notifies the BPEL engine. " +
            "The BPEL process receives the task completion callback and resumes execution from the wait state.",
            "SUCCESS"
        );

        loan.setDecisionNotes("Manually Approved: " + comments);
        approveLoan(loan);
    }

    public void rejectManualTask(Long loanId, String comments) {
        LoanApplication loan = loanRepository.findById(loanId).orElseThrow(() -> new IllegalArgumentException("Loan not found"));
        if (loan.getStatus() != LoanStatus.PENDING_MANUAL_REVIEW) {
            throw new IllegalStateException("Loan is not in PENDING_MANUAL_REVIEW state");
        }

        addLog(loanId,
            "Complete Underwriter Human Task",
            "Loan Officer processed manual task and submitted rejection. Comments: " + comments,
            "A loan officer logs into the BPM Worklist UI, claims the task, and clicks 'Reject'. " +
            "The Human Task Service completes the task, updates database, and sends a rejection callback to BPEL. " +
            "The BPEL process resumes execution and routes to the rejection branch.",
            "REJECTED"
        );

        loan.setDecisionNotes("Manually Rejected: " + comments);
        rejectLoan(loan);
    }

    public void cancelLoanWorkflow(Long loanId) {
        LoanApplication loan = loanRepository.findById(loanId).orElseThrow(() -> new IllegalArgumentException("Loan not found"));
        if (loan.getStatus() != LoanStatus.PENDING_MANUAL_REVIEW) {
            throw new IllegalStateException("Only loans in PENDING_MANUAL_REVIEW state can be cancelled.");
        }

        addLog(loanId,
            "Orchestration Received Applicant Cancellation Event",
            "Applicant submitted a request to cancel the loan application during underwriter review.",
            "The BPEL process is currently waiting in a <pick> activity. It receives a cancellation message on the 'cancelCorrelation' port " +
            "(simulating Event 2: <onMessage>). The process stops waiting for the underwriter callback and routes to the cancellation branch.",
            "CANCELLED"
        );

        loan.setStatus(LoanStatus.CANCELLED);
        loan.setDecision("CANCELLED");
        loan.setDecisionNotes("Application cancelled by user request.");
        loanRepository.save(loan);

        addLog(loanId,
            "Clean Up Task and Release Resources",
            "BPM Worklist Human Task instance has been automatically withdrawn.",
            "The cancellation branch in BPEL executes a task withdraw action, releasing the task from the BPM Task Service repository.",
            "SUCCESS"
        );

        sendNotification(loan);
    }

    private void approveLoan(LoanApplication loan) {
        loan.setStatus(LoanStatus.APPROVED);
        loan.setDecision("APPROVED");
        loan = loanRepository.save(loan);

        addLog(loan.getId(),
            "Approve Loan Application",
            "Loan status set to APPROVED.",
            "BPEL transition path executes the approval branch. It moves the process forward to prepare for fund disbursement.",
            "SUCCESS"
        );

        // Advance to Disbursement
        disburseFunds(loan);
    }

    private void rejectLoan(LoanApplication loan) {
        loan.setStatus(LoanStatus.REJECTED);
        loan.setDecision("REJECTED");
        loan = loanRepository.save(loan);

        addLog(loan.getId(),
            "Reject Loan Application",
            "Loan status set to REJECTED.",
            "BPEL transition path executes the rejection branch. It bypasses disbursement and routes directly to the notification stage.",
            "REJECTED"
        );

        // Advance to Notification
        sendNotification(loan);
    }

    private void disburseFunds(LoanApplication loan) {
        loan.setStatus(LoanStatus.DISBURSED);
        loan = loanRepository.save(loan);

        String disbursementMsg = "Sent payment instruction to core banking microservice.";
        try {
            Map<String, Object> request = new HashMap<>();
            request.put("loanId", loan.getId());
            request.put("loanAmount", loan.getLoanAmount());
            Map<String, Object> response = restTemplate.postForObject(disbursementServiceUrl, request, Map.class);
            if (response != null && response.containsKey("message")) {
                disbursementMsg = (String) response.get("message");
            }
        } catch (Exception e) {
            disbursementMsg = "Core Banking communication failure. Payment instruction offline: " + e.getMessage();
        }

        addLog(loan.getId(),
            "Disburse Funds to Core Banking",
            disbursementMsg,
            "BPEL invokes the CoreBankingDisbursementProxy on OSB. " +
            "OSB translates the SOAP message from BPEL into a JMS Queue message, sending it to the core banking ledger system asynchronously. " +
            "OSB handles transactional reliability (XA transactions) to ensure the message is delivered exactly once.",
            "SUCCESS"
        );

        sendNotification(loan);
    }

    private void sendNotification(LoanApplication loan) {
        String notificationMsg = "Triggered customer notification via notification-service.";
        try {
            Map<String, Object> request = new HashMap<>();
            request.put("loanId", loan.getId());
            request.put("applicantName", loan.getApplicantName());
            request.put("decision", loan.getDecision());
            Map<String, Object> response = restTemplate.postForObject(notificationServiceUrl, request, Map.class);
            if (response != null && response.containsKey("message")) {
                notificationMsg = (String) response.get("message");
            }
        } catch (Exception e) {
            notificationMsg = "Notification dispatch failed: " + e.getMessage();
        }

        addLog(loan.getId(),
            "Dispatch Customer Notification",
            notificationMsg,
            "BPEL invokes the User Messaging Service (UMS) adapter. " +
            "UMS coordinates email, SMS, and IM configurations, mapping the user's preferred communication channel to dispatch the notification template.",
            "SUCCESS"
        );

        // Log final completion
        addLog(loan.getId(),
            "Workflow Process Completed",
            "Loan workflow execution finalized. Final Decision: " + loan.getDecision() + ".",
            "The BPEL process reaches its <reply> activity, returning the final transaction payload back to OSB, " +
            "which then returns the HTTP response back to the calling client. The BPEL process instance terminates with a 'Completed' status.",
            "SUCCESS"
        );
    }

    private void addLog(Long loanId, String stageName, String springBootDetail, String osbSoaMapping, String status) {
        AuditLogEntry entry = new AuditLogEntry(loanId, stageName, springBootDetail, osbSoaMapping, status);
        auditRepository.save(entry);
    }
}
