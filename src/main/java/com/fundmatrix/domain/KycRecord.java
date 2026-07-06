package com.fundmatrix.domain;

import com.fundmatrix.common.BaseEntity;
import com.fundmatrix.domain.enums.KycStatus;
import com.fundmatrix.domain.enums.KycType;
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

import java.time.LocalDate;

/**
 * KYC verification record for an investor. Tracks the KYC mode, the supporting document,
 * verification date and compliance status.
 */
@Entity
@Table(name = "kyc_records", indexes = {
        @Index(name = "idx_kyc_investor", columnList = "investor_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KycRecord extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "investor_id", nullable = false)
    private User investor;

    @Enumerated(EnumType.STRING)
    @Column(name = "kyc_type", nullable = false, length = 20)
    private KycType kycType;

    @Column(name = "document_type", length = 60)
    private String documentType;

    @Column(name = "document_ref", length = 60)
    private String documentRef;

    @Column(name = "verified_date")
    private LocalDate verifiedDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "kyc_status", nullable = false, length = 20)
    private KycStatus kycStatus;
}
