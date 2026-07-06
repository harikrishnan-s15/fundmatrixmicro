package com.fundmatrix.service;

import com.fundmatrix.common.Calc;
import com.fundmatrix.common.exception.BusinessException;
import com.fundmatrix.common.exception.ResourceNotFoundException;
import com.fundmatrix.domain.DividendDeclaration;
import com.fundmatrix.domain.FolioHolding;
import com.fundmatrix.domain.InvestorDividendEntitlement;
import com.fundmatrix.domain.SchemeOption;
import com.fundmatrix.domain.enums.DividendStatus;
import com.fundmatrix.domain.enums.EntitlementStatus;
import com.fundmatrix.domain.enums.NotificationCategory;
import com.fundmatrix.domain.enums.OptionType;
import com.fundmatrix.domain.enums.PayoutMode;
import com.fundmatrix.dto.CreateDividendRequest;
import com.fundmatrix.dto.DividendDeclarationDto;
import com.fundmatrix.dto.EntitlementDto;
import com.fundmatrix.repository.DividendDeclarationRepository;
import com.fundmatrix.repository.FolioHoldingRepository;
import com.fundmatrix.repository.InvestorDividendEntitlementRepository;
import com.fundmatrix.repository.SchemeOptionRepository;
import com.fundmatrix.security.CurrentUserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/** Dividend declaration, entitlement computation and payout/reinvestment processing. */
@Service
public class DividendService {

    private final DividendDeclarationRepository declarationRepository;
    private final InvestorDividendEntitlementRepository entitlementRepository;
    private final SchemeOptionRepository optionRepository;
    private final FolioHoldingRepository holdingRepository;
    private final HoldingService holdingService;
    private final NotificationService notificationService;
    private final AuditService auditService;
    private final CurrentUserService currentUser;
    private final Mapper mapper;
    private final BigDecimal tdsRate;

    public DividendService(DividendDeclarationRepository declarationRepository,
                           InvestorDividendEntitlementRepository entitlementRepository,
                           SchemeOptionRepository optionRepository, FolioHoldingRepository holdingRepository,
                           HoldingService holdingService, NotificationService notificationService,
                           AuditService auditService, CurrentUserService currentUser, Mapper mapper,
                           @Value("${fundmatrix.tax.dividend-tds-rate}") BigDecimal tdsRate) {
        this.declarationRepository = declarationRepository;
        this.entitlementRepository = entitlementRepository;
        this.optionRepository = optionRepository;
        this.holdingRepository = holdingRepository;
        this.holdingService = holdingService;
        this.notificationService = notificationService;
        this.auditService = auditService;
        this.currentUser = currentUser;
        this.mapper = mapper;
        this.tdsRate = tdsRate;
    }

    @Transactional
    public DividendDeclarationDto declare(CreateDividendRequest req) {
        SchemeOption option = optionRepository.findById(req.optionId())
                .orElseThrow(() -> ResourceNotFoundException.of("SchemeOption", req.optionId()));
        if (option.getOptionType() == OptionType.GROWTH) {
            throw new BusinessException("Dividends cannot be declared on a Growth option");
        }
        DividendDeclaration declaration = DividendDeclaration.builder()
                .scheme(option.getScheme()).option(option)
                .recordDate(req.recordDate())
                .dividendPerUnit(Calc.rate(req.dividendPerUnit()))
                .totalDistributionAmount(BigDecimal.ZERO)
                .declaredById(currentUser.getId())
                .status(DividendStatus.DECLARED)
                .build();
        declaration = declarationRepository.save(declaration);
        auditService.record("DIVIDEND_DECLARE", "DividendDeclaration", declaration.getId(),
                "Declared " + req.dividendPerUnit() + "/unit on option " + option.getId());
        return toDto(declaration);
    }

    @Transactional
    public List<EntitlementDto> computeEntitlements(Long declarationId) {
        DividendDeclaration declaration = require(declarationId);
        if (declaration.getStatus() == DividendStatus.PROCESSED
                || declaration.getStatus() == DividendStatus.CANCELLED) {
            throw new BusinessException("Cannot recompute a " + declaration.getStatus() + " declaration");
        }
        // Recompute from scratch.
        entitlementRepository.deleteAll(entitlementRepository.findByDeclaration_Id(declarationId));

        BigDecimal dpu = declaration.getDividendPerUnit();
        boolean reinvest = declaration.getOption().getOptionType() == OptionType.DIVIDEND_REINVESTMENT;
        BigDecimal totalGross = BigDecimal.ZERO;

        List<FolioHolding> holdings = holdingRepository.findByOption_Id(declaration.getOption().getId());
        for (FolioHolding h : holdings) {
            BigDecimal units = Calc.nz(h.getUnitsHeld());
            if (units.signum() <= 0) {
                continue;
            }
            BigDecimal gross = Calc.amountFor(units, dpu);
            BigDecimal tax = Calc.percentOf(gross, tdsRate);
            BigDecimal net = Calc.money(gross.subtract(tax));
            InvestorDividendEntitlement e = InvestorDividendEntitlement.builder()
                    .declaration(declaration).folio(h.getFolio())
                    .unitsOnRecordDate(Calc.units(units))
                    .grossDividend(gross).taxDeducted(tax).netDividend(net)
                    .payoutMode(reinvest ? PayoutMode.REINVESTMENT : PayoutMode.BANK_CREDIT)
                    .status(EntitlementStatus.COMPUTED)
                    .build();
            entitlementRepository.save(e);
            totalGross = totalGross.add(gross);
        }
        declaration.setTotalDistributionAmount(Calc.money(totalGross));
        declarationRepository.save(declaration);
        auditService.record("DIVIDEND_COMPUTE", "DividendDeclaration", declarationId,
                "Computed entitlements, total gross " + totalGross);
        return getEntitlements(declarationId);
    }

    @Transactional
    public DividendDeclarationDto approve(Long declarationId) {
        DividendDeclaration declaration = require(declarationId);
        if (declaration.getStatus() != DividendStatus.DECLARED) {
            throw new BusinessException("Only DECLARED dividends can be approved");
        }
        declaration.setStatus(DividendStatus.APPROVED);
        auditService.record("DIVIDEND_APPROVE", "DividendDeclaration", declarationId, "Approved");
        return toDto(declarationRepository.save(declaration));
    }

    @Transactional
    public DividendDeclarationDto process(Long declarationId) {
        DividendDeclaration declaration = require(declarationId);
        if (declaration.getStatus() != DividendStatus.APPROVED) {
            throw new BusinessException("Only APPROVED dividends can be processed");
        }
        List<InvestorDividendEntitlement> entitlements =
                entitlementRepository.findByDeclaration_Id(declarationId);
        if (entitlements.isEmpty()) {
            throw new BusinessException("No entitlements computed for this declaration");
        }
        BigDecimal nav = holdingService.latestNavOrNull(declaration.getOption().getId());

        for (InvestorDividendEntitlement e : entitlements) {
            if (e.getStatus() != EntitlementStatus.COMPUTED) {
                continue;
            }
            if (e.getPayoutMode() == PayoutMode.REINVESTMENT && nav != null && nav.signum() > 0) {
                BigDecimal addUnits = Calc.unitsFor(e.getNetDividend(), nav);
                holdingService.creditUnits(e.getFolio(), declaration.getScheme(),
                        declaration.getOption(), addUnits, e.getNetDividend(), nav);
                e.setStatus(EntitlementStatus.REINVESTED);
                notificationService.notify(e.getFolio().getInvestor(), NotificationCategory.DIVIDEND,
                        "Dividend of " + e.getNetDividend() + " reinvested as " + addUnits
                                + " units in " + declaration.getScheme().getSchemeName());
            } else {
                e.setStatus(EntitlementStatus.DISBURSED);
                notificationService.notify(e.getFolio().getInvestor(), NotificationCategory.DIVIDEND,
                        "Dividend of " + e.getNetDividend() + " credited to your registered bank account ("
                                + declaration.getScheme().getSchemeName() + ")");
            }
            entitlementRepository.save(e);
        }
        declaration.setStatus(DividendStatus.PROCESSED);
        auditService.record("DIVIDEND_PROCESS", "DividendDeclaration", declarationId,
                "Processed " + entitlements.size() + " entitlements");
        return toDto(declarationRepository.save(declaration));
    }

    @Transactional
    public DividendDeclarationDto cancel(Long declarationId) {
        DividendDeclaration declaration = require(declarationId);
        if (declaration.getStatus() == DividendStatus.PROCESSED) {
            throw new BusinessException("Processed dividends cannot be cancelled");
        }
        declaration.setStatus(DividendStatus.CANCELLED);
        auditService.record("DIVIDEND_CANCEL", "DividendDeclaration", declarationId, "Cancelled");
        return toDto(declarationRepository.save(declaration));
    }

    @Transactional(readOnly = true)
    public List<DividendDeclarationDto> list(DividendStatus status) {
        List<DividendDeclaration> declarations = (status == null)
                ? declarationRepository.findAll() : declarationRepository.findByStatus(status);
        return declarations.stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<EntitlementDto> getEntitlements(Long declarationId) {
        return entitlementRepository.findByDeclaration_Id(declarationId).stream()
                .map(mapper::toEntitlementDto).toList();
    }

    @Transactional(readOnly = true)
    public List<EntitlementDto> myEntitlements() {
        return entitlementRepository.findByFolio_Investor_IdOrderByIdDesc(currentUser.getId()).stream()
                .map(mapper::toEntitlementDto).toList();
    }

    private DividendDeclaration require(Long id) {
        return declarationRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("DividendDeclaration", id));
    }

    private DividendDeclarationDto toDto(DividendDeclaration d) {
        return mapper.toDividendDto(d, entitlementRepository.countByDeclaration_Id(d.getId()));
    }
}
