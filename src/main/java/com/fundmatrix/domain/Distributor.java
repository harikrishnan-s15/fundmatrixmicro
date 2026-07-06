package com.fundmatrix.domain;

import com.fundmatrix.common.BaseEntity;
import com.fundmatrix.domain.enums.CommissionModel;
import com.fundmatrix.domain.enums.DistributorStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Empanelled distributor / IFA who services investor folios and earns commission.
 * Optionally linked to a login {@link User} with role {@code DISTRIBUTOR}.
 */
@Entity
@Table(name = "distributors")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Distributor extends BaseEntity {

    @Column(nullable = false, length = 160)
    private String name;

    /** AMFI Registration Number. */
    @Column(name = "arn_number", length = 30)
    private String arnNumber;

    /** Employee Unique Identification Number. */
    @Column(name = "euin_number", length = 30)
    private String euinNumber;

    @Column(name = "empanelment_date")
    private LocalDate empanelmentDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "commission_model", nullable = false, length = 20)
    private CommissionModel commissionModel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DistributorStatus status;

    /** Login account for this distributor, when one exists. */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;
}
