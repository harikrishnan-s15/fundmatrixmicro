package com.fundmatrix.transaction.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private Long folioId;
    private Long schemeId;
    private Long userId;
    
    @Enumerated(EnumType.STRING)
    private TransactionType type;
    
    @Enumerated(EnumType.STRING)
    private TransactionStatus status;
    
    private BigDecimal amount;
    private BigDecimal units;
    private BigDecimal nav;
    
    private LocalDateTime transactionDate;
    private LocalDateTime createdDate;
    private LocalDateTime lastModifiedDate;
    
    private String remarks;
}
