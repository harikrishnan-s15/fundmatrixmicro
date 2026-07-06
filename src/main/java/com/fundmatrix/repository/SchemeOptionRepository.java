package com.fundmatrix.repository;

import com.fundmatrix.domain.SchemeOption;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SchemeOptionRepository extends JpaRepository<SchemeOption, Long> {

    List<SchemeOption> findByScheme_Id(Long schemeId);

    Optional<SchemeOption> findByIsinIgnoreCase(String isin);

    boolean existsByIsinIgnoreCase(String isin);
}
