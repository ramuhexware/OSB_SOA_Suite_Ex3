package com.example.loanservice.repository;

import com.example.loanservice.model.AuditLogEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLogEntry, Long> {
    List<AuditLogEntry> findByLoanApplicationIdOrderByTimestampAsc(Long loanApplicationId);
}
