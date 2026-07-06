package com.fundmatrix.repository;

import com.fundmatrix.domain.Transaction;
import com.fundmatrix.domain.enums.TransactionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Optional<Transaction> findByTransactionRef(String transactionRef);

    List<Transaction> findByFolio_IdOrderByTransactionDateDesc(Long folioId);

    List<Transaction> findByFolio_Investor_IdOrderByTransactionDateDesc(Long investorId);

    List<Transaction> findByFolio_Distributor_IdOrderByTransactionDateDesc(Long distributorId);

    List<Transaction> findByStatusOrderByTransactionDateAsc(TransactionStatus status);

    List<Transaction> findByStatusInOrderByTransactionDateAsc(List<TransactionStatus> statuses);
}
