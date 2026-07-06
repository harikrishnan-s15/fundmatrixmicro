package com.fundmatrix.repository;

import com.fundmatrix.domain.SipMandate;
import com.fundmatrix.domain.enums.SipStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SipMandateRepository extends JpaRepository<SipMandate, Long> {

    Optional<SipMandate> findByMandateRef(String mandateRef);

    List<SipMandate> findByFolio_Id(Long folioId);

    List<SipMandate> findByFolio_Investor_Id(Long investorId);

    List<SipMandate> findByStatus(SipStatus status);
}
