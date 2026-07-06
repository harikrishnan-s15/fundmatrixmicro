package com.fundmatrix.domain;

import com.fundmatrix.common.BaseEntity;
import com.fundmatrix.domain.enums.SipFrequency;
import com.fundmatrix.domain.enums.SipStatus;
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
import java.time.LocalDate;

/**
 * A Systematic Investment Plan mandate: a recurring subscription instruction for a
 * folio/scheme/option at a fixed amount and frequency.
 */
@Entity
@Table(name = "sip_mandates", indexes = {
        @Index(name = "idx_sip_folio", columnList = "folio_id"),
        @Index(name = "idx_sip_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SipMandate extends BaseEntity {

    /** Human-readable mandate reference, e.g. "SIP000123"; assigned from the id after insert. */
    @Column(name = "mandate_ref", unique = true, length = 30)
    private String mandateRef;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "folio_id", nullable = false)
    private InvestorFolio folio;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "scheme_id", nullable = false)
    private FundScheme scheme;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "option_id", nullable = false)
    private SchemeOption option;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SipFrequency frequency;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    /** Total number of instalments planned. */
    @Column(name = "instalment_count")
    private Integer instalmentCount;

    /** Number of instalments already executed. */
    @Column(name = "instalments_executed", nullable = false)
    private Integer instalmentsExecuted;

    /** Date of the next scheduled instalment. */
    @Column(name = "next_instalment_date")
    private LocalDate nextInstalmentDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SipStatus status;
}
