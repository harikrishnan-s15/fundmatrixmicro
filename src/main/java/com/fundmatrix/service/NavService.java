package com.fundmatrix.service;

import com.fundmatrix.common.Calc;
import com.fundmatrix.common.exception.BusinessException;
import com.fundmatrix.common.exception.ResourceNotFoundException;
import com.fundmatrix.domain.FolioHolding;
import com.fundmatrix.domain.FundExpenseAccrual;
import com.fundmatrix.domain.FundScheme;
import com.fundmatrix.domain.NavRecord;
import com.fundmatrix.domain.SchemeOption;
import com.fundmatrix.domain.enums.ExpenseStatus;
import com.fundmatrix.domain.enums.NavStatus;
import com.fundmatrix.domain.enums.NotificationCategory;
import com.fundmatrix.dto.AumSummaryDto;
import com.fundmatrix.dto.NavRecordDto;
import com.fundmatrix.dto.SaveNavRequest;
import com.fundmatrix.repository.FolioHoldingRepository;
import com.fundmatrix.repository.FundExpenseAccrualRepository;
import com.fundmatrix.repository.FundSchemeRepository;
import com.fundmatrix.repository.NavRecordRepository;
import com.fundmatrix.repository.SchemeOptionRepository;
import com.fundmatrix.security.CurrentUserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** NAV capture, publication (with holding revaluation) and AUM reporting. */
@Service
public class NavService {

    private final NavRecordRepository navRepository;
    private final SchemeOptionRepository optionRepository;
    private final FundSchemeRepository schemeRepository;
    private final FolioHoldingRepository holdingRepository;
    private final FundExpenseAccrualRepository accrualRepository;
    private final HoldingService holdingService;
    private final NotificationService notificationService;
    private final AuditService auditService;
    private final CurrentUserService currentUser;
    private final Mapper mapper;

    public NavService(NavRecordRepository navRepository, SchemeOptionRepository optionRepository,
                      FundSchemeRepository schemeRepository, FolioHoldingRepository holdingRepository,
                      FundExpenseAccrualRepository accrualRepository,
                      HoldingService holdingService, NotificationService notificationService,
                      AuditService auditService, CurrentUserService currentUser, Mapper mapper) {
        this.navRepository = navRepository;
        this.optionRepository = optionRepository;
        this.schemeRepository = schemeRepository;
        this.holdingRepository = holdingRepository;
        this.accrualRepository = accrualRepository;
        this.holdingService = holdingService;
        this.notificationService = notificationService;
        this.auditService = auditService;
        this.currentUser = currentUser;
        this.mapper = mapper;
    }

    @Transactional
    public NavRecordDto saveNavInput(SaveNavRequest req) {
        SchemeOption option = optionRepository.findById(req.optionId())
                .orElseThrow(() -> ResourceNotFoundException.of("SchemeOption", req.optionId()));

        NavRecord nav = navRepository.findByOption_IdAndNavDate(req.optionId(), req.navDate())
                .orElseGet(() -> NavRecord.builder()
                        .scheme(option.getScheme()).option(option).navDate(req.navDate())
                        .status(NavStatus.PROVISIONAL).build());

        nav.setNavValue(Calc.rate(req.navValue()));
        nav.setTotalAum(req.totalAum());
        nav.setTotalUnitsOutstanding(req.totalUnitsOutstanding());
        if (nav.getStatus() == NavStatus.PUBLISHED) {
            nav.setStatus(NavStatus.REVISED);
        }
        nav = navRepository.save(nav);
        auditService.record("NAV_INPUT", "NavRecord", nav.getId(),
                "Captured NAV " + nav.getNavValue() + " for option " + option.getId());
        return mapper.toNavDto(nav);
    }

    @Transactional
    public NavRecordDto publish(Long navId) {
        NavRecord nav = navRepository.findById(navId)
                .orElseThrow(() -> ResourceNotFoundException.of("NavRecord", navId));
        if (nav.getStatus() == NavStatus.PUBLISHED) {
            throw new BusinessException("NAV is already published");
        }

        // Derive AUM figures from holdings when not supplied.
        List<FolioHolding> holdings = holdingRepository.findByOption_Id(nav.getOption().getId());
        BigDecimal totalUnits = holdings.stream().map(h -> Calc.nz(h.getUnitsHeld()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Apply the scheme's un-applied accrued expenses to this NAV (real expense impact):
        // NAV is reduced by expense-per-unit, then those accruals are marked APPLIED so they
        // are deducted only once. (Phase-1: applied on the first option NAV published that day.)
        applyAccruedExpenses(nav, totalUnits);

        if (nav.getTotalUnitsOutstanding() == null) {
            nav.setTotalUnitsOutstanding(Calc.units(totalUnits));
        }
        if (nav.getTotalAum() == null) {
            nav.setTotalAum(Calc.amountFor(totalUnits, nav.getNavValue()));
        }
        nav.setStatus(NavStatus.PUBLISHED);
        nav.setPublishedById(currentUser.getId());
        nav = navRepository.save(nav);

        holdingService.revalueOption(nav.getOption().getId(), nav.getNavValue());

        // NAV publication alerts to affected investors.
        Set<Long> notified = new HashSet<>();
        for (FolioHolding h : holdings) {
            var investor = h.getFolio().getInvestor();
            if (investor != null && notified.add(investor.getId())) {
                notificationService.notify(investor, NotificationCategory.NAV,
                        "NAV for " + nav.getScheme().getSchemeName() + " published at "
                                + nav.getNavValue() + " (" + nav.getNavDate() + ")");
            }
        }
        auditService.record("NAV_PUBLISH", "NavRecord", nav.getId(),
                "Published NAV " + nav.getNavValue() + "; revalued " + holdings.size() + " holdings");
        return mapper.toNavDto(nav);
    }

    /** Reduces the NAV by the per-unit value of the scheme's un-applied accruals, then marks them APPLIED. */
    private void applyAccruedExpenses(NavRecord nav, BigDecimal totalUnits) {
        if (totalUnits == null || totalUnits.signum() <= 0) {
            return;
        }
        List<FundExpenseAccrual> accrued =
                accrualRepository.findByScheme_IdAndStatus(nav.getScheme().getId(), ExpenseStatus.ACCRUED);
        BigDecimal totalExpense = accrued.stream()
                .map(a -> Calc.nz(a.getAccrualAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (totalExpense.signum() <= 0) {
            return;
        }
        BigDecimal perUnit = totalExpense.divide(totalUnits, Calc.UNIT_SCALE, Calc.RM);
        BigDecimal reduced = Calc.rate(nav.getNavValue().subtract(perUnit).max(BigDecimal.ZERO));
        nav.setNavValue(reduced);
        accrued.forEach(a -> a.setStatus(ExpenseStatus.APPLIED));
        accrualRepository.saveAll(accrued);
        auditService.record("NAV_EXPENSE_APPLIED", "NavRecord", nav.getId(),
                "Deducted expenses " + Calc.money(totalExpense) + " (" + perUnit + "/unit); NAV -> " + reduced);
    }

    @Transactional(readOnly = true)
    public List<NavRecordDto> listByScheme(Long schemeId) {
        return navRepository.findByScheme_IdOrderByNavDateDesc(schemeId)
                .stream().map(mapper::toNavDto).toList();
    }

    @Transactional(readOnly = true)
    public List<NavRecordDto> listByOption(Long optionId) {
        return navRepository.findByOption_IdOrderByNavDateDesc(optionId)
                .stream().map(mapper::toNavDto).toList();
    }

    @Transactional(readOnly = true)
    public List<AumSummaryDto> aumSummary() {
        List<FundScheme> schemes = schemeRepository.findAll();
        List<AumSummaryDto> out = new ArrayList<>();
        for (FundScheme s : schemes) {
            BigDecimal aum = holdingRepository.sumCurrentValueByScheme(s.getId());
            BigDecimal units = holdingRepository.findByScheme_Id(s.getId()).stream()
                    .map(h -> Calc.nz(h.getUnitsHeld())).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal latestNav = navRepository.findByScheme_IdOrderByNavDateDesc(s.getId())
                    .stream().findFirst().map(NavRecord::getNavValue).orElse(null);
            out.add(new AumSummaryDto(s.getId(), s.getSchemeName(), s.getSchemeCode(), s.getCategory(),
                    latestNav, Calc.money(aum), Calc.units(units)));
        }
        return out;
    }
}
