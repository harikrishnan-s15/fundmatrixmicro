package com.fundmatrix.transaction.service;

import com.fundmatrix.commons.exception.BusinessException;
import com.fundmatrix.commons.exception.ResourceNotFoundException;
import com.fundmatrix.transaction.client.FolioServiceClient;
import com.fundmatrix.transaction.client.NavServiceClient;
import com.fundmatrix.transaction.client.UserServiceClient;
import com.fundmatrix.transaction.domain.Transaction;
import com.fundmatrix.transaction.domain.TransactionStatus;
import com.fundmatrix.transaction.domain.TransactionType;
import com.fundmatrix.transaction.dto.RedemptionRequest;
import com.fundmatrix.transaction.dto.SubscriptionRequest;
import com.fundmatrix.transaction.dto.TransactionDto;
import com.fundmatrix.transaction.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final UserServiceClient userServiceClient;
    private final FolioServiceClient folioServiceClient;
    private final NavServiceClient navServiceClient;

    public TransactionService(TransactionRepository transactionRepository,
                             UserServiceClient userServiceClient,
                             FolioServiceClient folioServiceClient,
                             NavServiceClient navServiceClient) {
        this.transactionRepository = transactionRepository;
        this.userServiceClient = userServiceClient;
        this.folioServiceClient = folioServiceClient;
        this.navServiceClient = navServiceClient;
    }

    @Transactional
    public TransactionDto subscription(SubscriptionRequest req) {
        var folio = folioServiceClient.getFolioById(req.folioId());
        if (folio == null) {
            throw new ResourceNotFoundException("Folio not found: " + req.folioId());
        }

        Transaction transaction = Transaction.builder()
                .folioId(req.folioId())
                .schemeId(req.schemeId())
                .userId(folio.userId())
                .type(TransactionType.SUBSCRIPTION)
                .status(TransactionStatus.PENDING)
                .amount(req.amount())
                .nav(req.nav())
                .transactionDate(LocalDateTime.now())
                .createdDate(LocalDateTime.now())
                .build();
        transaction = transactionRepository.save(transaction);
        return toTransactionDto(transaction);
    }

    @Transactional
    public TransactionDto redemption(RedemptionRequest req) {
        var folio = folioServiceClient.getFolioById(req.folioId());
        if (folio == null) {
            throw new ResourceNotFoundException("Folio not found: " + req.folioId());
        }

        Transaction transaction = Transaction.builder()
                .folioId(req.folioId())
                .schemeId(req.schemeId())
                .userId(folio.userId())
                .type(TransactionType.REDEMPTION)
                .status(TransactionStatus.PENDING)
                .units(req.units())
                .nav(req.nav())
                .transactionDate(LocalDateTime.now())
                .createdDate(LocalDateTime.now())
                .build();
        transaction = transactionRepository.save(transaction);
        return toTransactionDto(transaction);
    }

    @Transactional(readOnly = true)
    public TransactionDto getTransactionById(Long id) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Transaction", id));
        return toTransactionDto(transaction);
    }

    @Transactional(readOnly = true)
    public List<TransactionDto> getTransactionsByFolio(Long folioId) {
        return transactionRepository.findByFolioId(folioId).stream()
                .map(this::toTransactionDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TransactionDto> getTransactionsByUser(Long userId) {
        return transactionRepository.findByUserId(userId).stream()
                .map(this::toTransactionDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TransactionDto> getTransactionsByStatus(TransactionStatus status) {
        return transactionRepository.findByStatus(status).stream()
                .map(this::toTransactionDto)
                .toList();
    }

    @Transactional
    public TransactionDto approveTransaction(Long id) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Transaction", id));
        if (transaction.getStatus() != TransactionStatus.PENDING) {
            throw new BusinessException("Transaction cannot be approved from current status");
        }
        transaction.setStatus(TransactionStatus.APPROVED);
        transaction.setLastModifiedDate(LocalDateTime.now());
        transaction = transactionRepository.save(transaction);
        return toTransactionDto(transaction);
    }

    @Transactional
    public TransactionDto rejectTransaction(Long id, String reason) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Transaction", id));
        transaction.setStatus(TransactionStatus.REJECTED);
        transaction.setRemarks(reason);
        transaction.setLastModifiedDate(LocalDateTime.now());
        transaction = transactionRepository.save(transaction);
        return toTransactionDto(transaction);
    }

    private TransactionDto toTransactionDto(Transaction transaction) {
        return new TransactionDto(
                transaction.getId(),
                transaction.getFolioId(),
                transaction.getSchemeId(),
                transaction.getUserId(),
                transaction.getType(),
                transaction.getStatus(),
                transaction.getAmount(),
                transaction.getUnits(),
                transaction.getNav(),
                transaction.getTransactionDate(),
                transaction.getRemarks()
        );
    }
}
