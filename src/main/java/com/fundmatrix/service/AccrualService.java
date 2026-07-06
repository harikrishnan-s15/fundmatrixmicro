package com.fundmatrix.service;

import com.fundmatrix.common.Calc;
import com.fundmatrix.common.exception.BusinessException;
import com.fundmatrix.common.exception.ResourceNotFoundException;
import com.fundmatrix.domain.FundExpenseAccrual;
import com.fundmatrix.domain.FundScheme;
import com.fundmatrix.domain.enums.ExpenseStatus;
import com.fundmatrix.domain.enums.ExpenseType;
import com.fundmatrix.dto.CreateAccrualRequest;
import com.fundmatrix.dto.ExpenseAccrualDto;
import com.fundmatrix.dto.ExpenseComplianceDto;
import com.fundmatrix.repository.FolioHoldingRepository;
import com.fundmatrix.repository.FundExpenseAccrualRepository;
import com.fundmatrix.repository.FundSchemeRepository;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Fund-level expense accrual booking and reversal. */
@Service
public class AccrualService {

    private static final BigDecimal DAYS_IN_YEAR = BigDecimal.valueOf(365);

    private final FundExpenseAccrualRepository accrualRepository;
    private final FundSchemeRepository schemeRepository;
    private final FolioHoldingRepository holdingRepository;
    private final AuditService auditService;
    private final Mapper mapper;

    public AccrualService(FundExpenseAccrualRepository accrualRepository,
                          FundSchemeRepository schemeRepository, FolioHoldingRepository holdingRepository,
                          AuditService auditService, Mapper mapper) {
        this.accrualRepository = accrualRepository;
        this.schemeRepository = schemeRepository;
        this.holdingRepository = holdingRepository;
        this.auditService = auditService;
        this.mapper = mapper;
    }

    @Transactional
    public ExpenseAccrualDto create(CreateAccrualRequest req) {
        FundScheme scheme = schemeRepository.findById(req.schemeId())
                .orElseThrow(() -> ResourceNotFoundException.of("FundScheme", req.schemeId()));

        BigDecimal amount = req.accrualAmount();
        if (amount == null) {
            // Daily accrual on scheme AUM at the annualised rate.
            BigDecimal aum = Calc.money(holdingRepository.sumCurrentValueByScheme(scheme.getId()));
            amount = Calc.percentOf(aum, req.annualisedRate()).divide(DAYS_IN_YEAR, Calc.AMOUNT_SCALE, Calc.RM);
        }

        FundExpenseAccrual accrual = FundExpenseAccrual.builder()
                .scheme(scheme)
                .expenseType(req.expenseType())
                .annualisedRate(Calc.rate(req.annualisedRate()))
                .accrualAmount(Calc.money(amount))
                .accrualDate(req.accrualDate() != null ? req.accrualDate() : LocalDate.now())
                .status(ExpenseStatus.ACCRUED)
                .build();
        accrual = accrualRepository.save(accrual);
        auditService.record("ACCRUAL_CREATE", "FundExpenseAccrual", accrual.getId(),
                req.expenseType() + " accrual " + accrual.getAccrualAmount() + " for " + scheme.getSchemeName());
        return mapper.toAccrualDto(accrual);
    }

    @Transactional(readOnly = true)
    public List<ExpenseAccrualDto> listByScheme(Long schemeId) {
        return accrualRepository.findByScheme_IdOrderByAccrualDateDesc(schemeId)
                .stream().map(mapper::toAccrualDto).toList();
    }

    @Transactional
    public ExpenseAccrualDto reverse(Long id, String reason) {
        FundExpenseAccrual accrual = accrualRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("FundExpenseAccrual", id));
        if (accrual.getStatus() == ExpenseStatus.REVERSED) {
            throw new BusinessException("Accrual is already reversed");
        }
        accrual.setStatus(ExpenseStatus.REVERSED);
        accrual.setReversalReason(reason);
        auditService.record("ACCRUAL_REVERSE", "FundExpenseAccrual", id, "Reversed: " + reason);
        return mapper.toAccrualDto(accrualRepository.save(accrual));
    }

    /** Expense-ratio compliance: charged rate (sum of latest annualised rate per type) vs the TER limit. */
    @Transactional(readOnly = true)
    public ExpenseComplianceDto compliance(Long schemeId) {
        FundScheme scheme = schemeRepository.findById(schemeId)
                .orElseThrow(() -> ResourceNotFoundException.of("FundScheme", schemeId));
        Map<ExpenseType, BigDecimal> perType = new EnumMap<>(ExpenseType.class);
        accrualRepository.findByScheme_IdOrderByAccrualDateDesc(schemeId).stream()
                .filter(a -> a.getStatus() != ExpenseStatus.REVERSED)
                .forEach(a -> perType.merge(a.getExpenseType(), Calc.nz(a.getAnnualisedRate()), BigDecimal::max));
        BigDecimal charged = perType.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal limit = scheme.getExpenseRatio();
        BigDecimal util = null;
        String status;
        if (limit == null || limit.signum() <= 0) {
            status = "NO_LIMIT";
        } else {
            util = charged.multiply(BigDecimal.valueOf(100)).divide(limit, 2, RoundingMode.HALF_UP);
            status = charged.compareTo(limit) > 0 ? "BREACH"
                    : (util.compareTo(BigDecimal.valueOf(80)) >= 0 ? "WARN" : "OK");
        }
        return new ExpenseComplianceDto(scheme.getId(), scheme.getSchemeName(), limit,
                Calc.rate(charged), util, status);
    }
}
