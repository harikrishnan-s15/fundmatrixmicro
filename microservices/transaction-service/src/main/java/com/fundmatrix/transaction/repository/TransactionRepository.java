package com.fundmatrix.transaction.repository;

import com.fundmatrix.transaction.domain.Transaction;
import com.fundmatrix.transaction.domain.TransactionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findByFolioId(Long folioId);
    List<Transaction> findByUserId(Long userId);
    List<Transaction> findBySchemeId(Long schemeId);
    List<Transaction> findByStatus(TransactionStatus status);
    List<Transaction> findByTransactionDateBetween(LocalDateTime start, LocalDateTime end);
}
