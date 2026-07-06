package com.fundmatrix.domain;

import com.fundmatrix.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
import java.time.Instant;

/**
 * The unit holding of a folio in a specific scheme option. Maintains units held,
 * average cost NAV and (NAV-derived) current value with unrealised gain/loss.
 */
@Entity
@Table(name = "folio_holdings",
        uniqueConstraints = @UniqueConstraint(name = "uk_holding_folio_option",
                columnNames = {"folio_id", "option_id"}),
        indexes = {
                @Index(name = "idx_holding_folio", columnList = "folio_id"),
                @Index(name = "idx_holding_scheme", columnList = "scheme_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FolioHolding extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "folio_id", nullable = false)
    private InvestorFolio folio;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "scheme_id", nullable = false)
    private FundScheme scheme;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "option_id", nullable = false)
    private SchemeOption option;

    @Column(name = "units_held", nullable = false, precision = 19, scale = 4)
    private BigDecimal unitsHeld;

    @Column(name = "average_cost_nav", precision = 19, scale = 4)
    private BigDecimal averageCostNav;

    @Column(name = "current_value", precision = 19, scale = 2)
    private BigDecimal currentValue;

    @Column(name = "unrealised_gain_loss", precision = 19, scale = 2)
    private BigDecimal unrealisedGainLoss;

    @Column(name = "last_updated")
    private Instant lastUpdated;
}
