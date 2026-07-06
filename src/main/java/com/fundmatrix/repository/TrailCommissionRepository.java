package com.fundmatrix.repository;

import com.fundmatrix.domain.TrailCommission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TrailCommissionRepository extends JpaRepository<TrailCommission, Long> {

    List<TrailCommission> findByDistributor_IdOrderByPeriodDesc(Long distributorId);

    List<TrailCommission> findByPeriod(String period);

    Optional<TrailCommission> findByDistributor_IdAndScheme_IdAndPeriod(
            Long distributorId, Long schemeId, String period);
}
