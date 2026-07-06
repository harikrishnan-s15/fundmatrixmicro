package com.fundmatrix.domain;

import com.fundmatrix.common.BaseEntity;
import com.fundmatrix.domain.enums.NavStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Daily Net Asset Value for a scheme option, captured manually by a fund accountant.
 * Drives unit allotment and holding valuation once published.
 */
@Entity
@Table(name = "nav_records",
        uniqueConstraints = @UniqueConstraint(name = "uk_nav_option_date",
                columnNames = {"option_id", "nav_date"}),
        indexes = {
                @Index(name = "idx_nav_scheme_date", columnList = "scheme_id, nav_date"),
                @Index(name = "idx_nav_status", columnList = "status")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NavRecord extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "scheme_id", nullable = false)
    private FundScheme scheme;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "option_id", nullable = false)
    private SchemeOption option;

    @Column(name = "nav_date", nullable = false)
    private LocalDate navDate;

    @Column(name = "nav_value", nullable = false, precision = 19, scale = 4)
    private BigDecimal navValue;

    @Column(name = "total_aum", precision = 19, scale = 2)
    private BigDecimal totalAum;

    @Column(name = "total_units_outstanding", precision = 19, scale = 4)
    private BigDecimal totalUnitsOutstanding;

    /** Fund accountant who published this NAV. */
    @Column(name = "published_by_id")
    private Long publishedById;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NavStatus status;
}
