package com.fundmatrix.transaction.controller;

import com.fundmatrix.transaction.domain.TransactionStatus;
import com.fundmatrix.transaction.dto.RedemptionRequest;
import com.fundmatrix.transaction.dto.SubscriptionRequest;
import com.fundmatrix.transaction.dto.TransactionDto;
import com.fundmatrix.transaction.service.TransactionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<TransactionDto> getTransaction(@PathVariable Long id) {
        return ResponseEntity.ok(transactionService.getTransactionById(id));
    }

    @GetMapping("/folio/{folioId}")
    public ResponseEntity<List<TransactionDto>> getTransactionsByFolio(@PathVariable Long folioId) {
        return ResponseEntity.ok(transactionService.getTransactionsByFolio(folioId));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<TransactionDto>> getTransactionsByUser(@PathVariable Long userId) {
        return ResponseEntity.ok(transactionService.getTransactionsByUser(userId));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<TransactionDto>> getTransactionsByStatus(@PathVariable TransactionStatus status) {
        return ResponseEntity.ok(transactionService.getTransactionsByStatus(status));
    }

    @PostMapping("/subscribe")
    public ResponseEntity<TransactionDto> subscribe(@RequestBody SubscriptionRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(transactionService.subscription(req));
    }

    @PostMapping("/redeem")
    public ResponseEntity<TransactionDto> redeem(@RequestBody RedemptionRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(transactionService.redemption(req));
    }

    @PutMapping("/{id}/approve")
    public ResponseEntity<TransactionDto> approveTransaction(@PathVariable Long id) {
        return ResponseEntity.ok(transactionService.approveTransaction(id));
    }

    @PutMapping("/{id}/reject")
    public ResponseEntity<TransactionDto> rejectTransaction(@PathVariable Long id, @RequestParam String reason) {
        return ResponseEntity.ok(transactionService.rejectTransaction(id, reason));
    }
}
