package com.fundmatrix.repository;

import com.fundmatrix.domain.DividendDeclaration;
import com.fundmatrix.domain.enums.DividendStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DividendDeclarationRepository extends JpaRepository<DividendDeclaration, Long> {

    List<DividendDeclaration> findByStatus(DividendStatus status);

    List<DividendDeclaration> findByOption_IdOrderByRecordDateDesc(Long optionId);

    List<DividendDeclaration> findByScheme_IdOrderByRecordDateDesc(Long schemeId);
}
