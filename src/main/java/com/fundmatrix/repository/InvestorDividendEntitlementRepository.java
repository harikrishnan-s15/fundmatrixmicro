package com.fundmatrix.repository;

import com.fundmatrix.domain.InvestorDividendEntitlement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InvestorDividendEntitlementRepository
        extends JpaRepository<InvestorDividendEntitlement, Long> {

    List<InvestorDividendEntitlement> findByDeclaration_Id(Long declarationId);

    List<InvestorDividendEntitlement> findByFolio_Id(Long folioId);

    List<InvestorDividendEntitlement> findByFolio_Investor_IdOrderByIdDesc(Long investorId);

    long countByDeclaration_Id(Long declarationId);
}
