package com.fundmatrix.repository;

import com.fundmatrix.domain.KycRecord;
import com.fundmatrix.domain.enums.KycStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface KycRecordRepository extends JpaRepository<KycRecord, Long> {

    List<KycRecord> findByInvestor_Id(Long investorId);

    List<KycRecord> findByKycStatus(KycStatus kycStatus);

    /** True when the investor has at least one KYC record in the given status. */
    boolean existsByInvestor_IdAndKycStatus(Long investorId, KycStatus kycStatus);

    long countByKycStatus(KycStatus kycStatus);
}
