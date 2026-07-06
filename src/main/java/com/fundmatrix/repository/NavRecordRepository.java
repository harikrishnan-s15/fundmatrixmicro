package com.fundmatrix.repository;

import com.fundmatrix.domain.NavRecord;
import com.fundmatrix.domain.enums.NavStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface NavRecordRepository extends JpaRepository<NavRecord, Long> {

    List<NavRecord> findByScheme_IdOrderByNavDateDesc(Long schemeId);

    List<NavRecord> findByOption_IdOrderByNavDateDesc(Long optionId);

    Optional<NavRecord> findByOption_IdAndNavDate(Long optionId, LocalDate navDate);

    /** Latest published NAV for a scheme option — the applicable NAV for allotment. */
    Optional<NavRecord> findTopByOption_IdAndStatusOrderByNavDateDesc(Long optionId, NavStatus status);

    Optional<NavRecord> findTopByOption_IdOrderByNavDateDesc(Long optionId);
}
