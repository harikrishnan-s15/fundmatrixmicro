package com.fundmatrix.repository;

import com.fundmatrix.domain.Distributor;
import com.fundmatrix.domain.enums.DistributorStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DistributorRepository extends JpaRepository<Distributor, Long> {

    Optional<Distributor> findByArnNumberIgnoreCase(String arnNumber);

    Optional<Distributor> findByUser_Id(Long userId);

    boolean existsByArnNumberIgnoreCase(String arnNumber);

    List<Distributor> findByStatus(DistributorStatus status);
}
