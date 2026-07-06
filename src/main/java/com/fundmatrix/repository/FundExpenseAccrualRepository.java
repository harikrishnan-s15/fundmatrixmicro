package com.fundmatrix.repository;

import com.fundmatrix.domain.FundExpenseAccrual;
import com.fundmatrix.domain.enums.ExpenseStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FundExpenseAccrualRepository extends JpaRepository<FundExpenseAccrual, Long> {

    List<FundExpenseAccrual> findByScheme_IdOrderByAccrualDateDesc(Long schemeId);

    List<FundExpenseAccrual> findByScheme_IdAndStatus(Long schemeId, ExpenseStatus status);
}
