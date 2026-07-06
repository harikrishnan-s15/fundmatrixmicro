package com.fundmatrix.repository;

import com.fundmatrix.domain.SwpMandate;
import com.fundmatrix.domain.enums.SipStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SwpMandateRepository extends JpaRepository<SwpMandate, Long> {

    List<SwpMandate> findByFolio_Investor_Id(Long investorId);

    List<SwpMandate> findByStatus(SipStatus status);
}
