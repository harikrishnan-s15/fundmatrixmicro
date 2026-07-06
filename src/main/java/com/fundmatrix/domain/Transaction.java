package com.fundmatrix.domain;

import com.fundmatrix.common.BaseEntity;
import com.fundmatrix.domain.enums.CutOffStatus;
import com.fundmatrix.domain.enums.TransactionStatus;
import com.fundmatrix.domain.enums.TransactionType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * A financial transaction against a folio — subscription, redemption, switch, SIP/SWP
 * instalment or dividend. NAV applicability is governed by cut-off timing.
 */
@Entity
@Table(name = "transactions", indexes = {
        @Index(name = "idx_txn_ref", columnList = "transaction_ref", unique = true),
        @Index(name = "idx_txn_folio", columnList = "folio_id"),
        @Index(name = "idx_txn_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction extends BaseEntity {

    /** Human-readable transaction reference, e.g. "TXN00000123"; assigned from the id after insert. */
    @Column(name = "transaction_ref", unique = true, length = 30)
    private String transactionRef;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "folio_id", nullable = false)
    private InvestorFolio folio;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "scheme_id", nullable = false)
    private FundScheme scheme;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "option_id", nullable = false)
    private SchemeOption option;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 20)
    private TransactionType transactionType;

    /** Gross monetary amount (for subscriptions; computed proceeds for redemptions). */
    @Column(precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(precision = 19, scale = 4)
    private BigDecimal units;

    @Column(name = "applicable_nav", precision = 19, scale = 4)
    private BigDecimal applicableNav;

    @Column(name = "transaction_date", nullable = false)
    private Instant transactionDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "cut_off_status", length = 20)
    private CutOffStatus cutOffStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionStatus status;

    /** Exit load deducted on redemption, if any. */
    @Column(name = "exit_load_amount", precision = 19, scale = 2)
    private BigDecimal exitLoadAmount;

    /** Free-text remark (rejection reason, switch linkage, SIP reference, etc.). */
    @Column(length = 255)
    private String remarks;

    /** Links a SIP/SWP instalment back to its originating mandate. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sip_mandate_id")
    private SipMandate sipMandate;
}
