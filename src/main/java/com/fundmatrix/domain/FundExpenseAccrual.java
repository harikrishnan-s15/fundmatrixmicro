package com.fundmatrix.domain;

import com.fundmatrix.common.BaseEntity;
import com.fundmatrix.domain.enums.ExpenseStatus;
import com.fundmatrix.domain.enums.ExpenseType;
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
 * A fund-level expense accrual (management fee, trustee fee, audit, custody, distribution)
 * booked against a scheme on a given date at an annualised rate.
 */
@Entity
@Table(name = "fund_expense_accruals", indexes = {
        @Index(name = "idx_accrual_scheme_date", columnList = "scheme_id, accrual_date")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FundExpenseAccrual extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "scheme_id", nullable = false)
    private FundScheme scheme;

    @Enumerated(EnumType.STRING)
    @Column(name = "expense_type", nullable = false, length = 30)
    private ExpenseType expenseType;

    @Column(name = "accrual_amount", precision = 19, scale = 2)
    private BigDecimal accrualAmount;

    @Column(name = "accrual_date", nullable = false)
    private LocalDate accrualDate;

    @Column(name = "annualised_rate", precision = 9, scale = 4)
    private BigDecimal annualisedRate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ExpenseStatus status;

    /** Mandatory reason captured when an accrual is reversed. */
    @Column(name = "reversal_reason", length = 255)
    private String reversalReason;
}
