package com.fundmatrix.domain;

import com.fundmatrix.common.BaseEntity;
import com.fundmatrix.domain.enums.FolioStatus;
import com.fundmatrix.domain.enums.ModeOfHolding;
import com.fundmatrix.domain.enums.TaxStatus;
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

/**
 * An investor's account (folio) within the AMC, mapped to an owning investor and an
 * optional servicing distributor. Holds tax status, holding mode, nominee and bank details.
 */
@Entity
@Table(name = "investor_folios", indexes = {
        @Index(name = "idx_folio_number", columnList = "folio_number", unique = true),
        @Index(name = "idx_folio_investor", columnList = "investor_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvestorFolio extends BaseEntity {

    /** Human-readable folio number, e.g. "FOL00001"; assigned from the id immediately after insert. */
    @Column(name = "folio_number", unique = true, length = 30)
    private String folioNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "investor_id", nullable = false)
    private User investor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "distributor_id")
    private Distributor distributor;

    @Enumerated(EnumType.STRING)
    @Column(name = "tax_status", nullable = false, length = 20)
    private TaxStatus taxStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "mode_of_holding", nullable = false, length = 30)
    private ModeOfHolding modeOfHolding;

    @Column(name = "nominee_details", length = 255)
    private String nomineeDetails;

    @Column(name = "bank_account_ref", length = 60)
    private String bankAccountRef;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FolioStatus status;
}
