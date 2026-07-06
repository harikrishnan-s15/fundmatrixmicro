package com.fundmatrix.repository;

import com.fundmatrix.domain.InvestorFolio;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InvestorFolioRepository extends JpaRepository<InvestorFolio, Long> {

    Optional<InvestorFolio> findByFolioNumber(String folioNumber);

    boolean existsByFolioNumber(String folioNumber);

    List<InvestorFolio> findByInvestor_Id(Long investorId);

    List<InvestorFolio> findByDistributor_Id(Long distributorId);
}
