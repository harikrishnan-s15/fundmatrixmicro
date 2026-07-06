package com.fundmatrix.repository;

import com.fundmatrix.domain.Allotment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AllotmentRepository extends JpaRepository<Allotment, Long> {

    Optional<Allotment> findByTransaction_Id(Long transactionId);
}
