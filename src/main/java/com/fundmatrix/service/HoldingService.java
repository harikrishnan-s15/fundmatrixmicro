package com.fundmatrix.service;

import com.fundmatrix.common.Calc;
import com.fundmatrix.common.exception.BusinessException;
import com.fundmatrix.domain.FolioHolding;
import com.fundmatrix.domain.FundScheme;
import com.fundmatrix.domain.InvestorFolio;
import com.fundmatrix.domain.NavRecord;
import com.fundmatrix.domain.SchemeOption;
import com.fundmatrix.domain.enums.NavStatus;
import com.fundmatrix.repository.FolioHoldingRepository;
import com.fundmatrix.repository.NavRecordRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Central authority for unit-holding mathematics: crediting/debiting units, weighted
 * average cost maintenance, and NAV-based revaluation. Keeps allotment, dividend
 * reinvestment and NAV publication consistent.
 */
@Service
public class HoldingService {

    private final FolioHoldingRepository holdingRepository;
    private final NavRecordRepository navRecordRepository;

    public HoldingService(FolioHoldingRepository holdingRepository,
                          NavRecordRepository navRecordRepository) {
        this.holdingRepository = holdingRepository;
        this.navRecordRepository = navRecordRepository;
    }

    /** Latest published NAV for an option, or throws if none has been published. */
    @Transactional(readOnly = true)
    public BigDecimal requirePublishedNav(Long optionId) {
        return navRecordRepository
                .findTopByOption_IdAndStatusOrderByNavDateDesc(optionId, NavStatus.PUBLISHED)
                .map(NavRecord::getNavValue)
                .orElseThrow(() -> new BusinessException(
                        "No published NAV available for option " + optionId + "; allotment cannot proceed"));
    }

    /** Latest NAV (any status) for valuation purposes, or null when unavailable. */
    @Transactional(readOnly = true)
    public BigDecimal latestNavOrNull(Long optionId) {
        return navRecordRepository.findTopByOption_IdOrderByNavDateDesc(optionId)
                .map(NavRecord::getNavValue).orElse(null);
    }

    /**
     * Adds units to a holding (creating it if necessary) and updates the weighted-average
     * cost using the invested amount, then revalues at the given NAV.
     */
    @Transactional
    public FolioHolding creditUnits(InvestorFolio folio, FundScheme scheme, SchemeOption option,
                                    BigDecimal addUnits, BigDecimal investedAmount, BigDecimal navValue) {
        FolioHolding holding = holdingRepository
                .findByFolio_IdAndOption_Id(folio.getId(), option.getId())
                .orElseGet(() -> FolioHolding.builder()
                        .folio(folio).scheme(scheme).option(option)
                        .unitsHeld(BigDecimal.ZERO).averageCostNav(BigDecimal.ZERO)
                        .build());

        BigDecimal oldUnits = Calc.nz(holding.getUnitsHeld());
        BigDecimal oldCost = oldUnits.multiply(Calc.nz(holding.getAverageCostNav()));
        BigDecimal newUnits = oldUnits.add(addUnits);
        BigDecimal newCost = oldCost.add(Calc.nz(investedAmount));

        holding.setUnitsHeld(Calc.units(newUnits));
        holding.setAverageCostNav(newUnits.signum() > 0
                ? newCost.divide(newUnits, Calc.UNIT_SCALE, Calc.RM)
                : BigDecimal.ZERO);
        revalue(holding, navValue);
        return holdingRepository.save(holding);
    }

    /** Removes units from a holding; average cost is unchanged on redemption. */
    @Transactional
    public FolioHolding debitUnits(InvestorFolio folio, SchemeOption option,
                                   BigDecimal redeemUnits, BigDecimal navValue) {
        FolioHolding holding = holdingRepository
                .findByFolio_IdAndOption_Id(folio.getId(), option.getId())
                .orElseThrow(() -> new BusinessException("No holding to redeem for this option"));

        BigDecimal available = Calc.nz(holding.getUnitsHeld());
        if (redeemUnits.compareTo(available) > 0) {
            throw new BusinessException("Insufficient units: holding " + available
                    + ", requested " + redeemUnits);
        }
        holding.setUnitsHeld(Calc.units(available.subtract(redeemUnits)));
        revalue(holding, navValue);
        return holdingRepository.save(holding);
    }

    /** Revalues every holding in an option to a new NAV — used on NAV publication. */
    @Transactional
    public int revalueOption(Long optionId, BigDecimal navValue) {
        List<FolioHolding> holdings = holdingRepository.findByOption_Id(optionId);
        for (FolioHolding h : holdings) {
            revalue(h, navValue);
        }
        holdingRepository.saveAll(holdings);
        return holdings.size();
    }

    private void revalue(FolioHolding holding, BigDecimal navValue) {
        BigDecimal units = Calc.nz(holding.getUnitsHeld());
        if (navValue != null) {
            BigDecimal value = Calc.amountFor(units, navValue);
            holding.setCurrentValue(value);
            BigDecimal cost = units.multiply(Calc.nz(holding.getAverageCostNav()));
            holding.setUnrealisedGainLoss(Calc.money(value.subtract(cost)));
        }
        holding.setLastUpdated(Instant.now());
    }
}
