package com.fundmatrix.domain;

import com.fundmatrix.common.BaseEntity;
import com.fundmatrix.domain.enums.DividendStatus;
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
 * A dividend declaration at scheme-option level: dividend per unit as of a record date,
 * from which investor-wise entitlements are computed.
 */
@Entity
@Table(name = "dividend_declarations", indexes = {
        @Index(name = "idx_dividend_option", columnList = "option_id"),
        @Index(name = "idx_dividend_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DividendDeclaration extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "scheme_id", nullable = false)
    private FundScheme scheme;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "option_id", nullable = false)
    private SchemeOption option;

    @Column(name = "record_date", nullable = false)
    private LocalDate recordDate;

    @Column(name = "dividend_per_unit", nullable = false, precision = 19, scale = 4)
    private BigDecimal dividendPerUnit;

    @Column(name = "total_distribution_amount", precision = 19, scale = 2)
    private BigDecimal totalDistributionAmount;

    @Column(name = "declared_by_id")
    private Long declaredById;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DividendStatus status;
}
