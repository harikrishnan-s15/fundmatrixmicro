package com.fundmatrix.service;

import com.fundmatrix.common.Calc;
import com.fundmatrix.common.exception.BusinessException;
import com.fundmatrix.common.exception.ResourceNotFoundException;
import com.fundmatrix.domain.Distributor;
import com.fundmatrix.domain.FundScheme;
import com.fundmatrix.domain.TrailCommission;
import com.fundmatrix.domain.enums.CommissionStatus;
import com.fundmatrix.domain.enums.NotificationCategory;
import com.fundmatrix.dto.ComputeCommissionRequest;
import com.fundmatrix.dto.DistributorDashboardDto;
import com.fundmatrix.dto.TrailCommissionDto;
import com.fundmatrix.repository.DistributorRepository;
import com.fundmatrix.repository.FolioHoldingRepository;
import com.fundmatrix.repository.FundSchemeRepository;
import com.fundmatrix.repository.InvestorFolioRepository;
import com.fundmatrix.repository.TrailCommissionRepository;
import com.fundmatrix.security.CurrentUserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Trail commission computation, approval and payout, plus the distributor AUM dashboard. */
@Service
public class CommissionService {

    private static final BigDecimal MONTHS = BigDecimal.valueOf(12);

    private final TrailCommissionRepository commissionRepository;
    private final DistributorRepository distributorRepository;
    private final FundSchemeRepository schemeRepository;
    private final FolioHoldingRepository holdingRepository;
    private final InvestorFolioRepository folioRepository;
    private final NotificationService notificationService;
    private final AuditService auditService;
    private final CurrentUserService currentUser;
    private final Mapper mapper;

    public CommissionService(TrailCommissionRepository commissionRepository,
                             DistributorRepository distributorRepository,
                             FundSchemeRepository schemeRepository, FolioHoldingRepository holdingRepository,
                             InvestorFolioRepository folioRepository, NotificationService notificationService,
                             AuditService auditService, CurrentUserService currentUser, Mapper mapper) {
        this.commissionRepository = commissionRepository;
        this.distributorRepository = distributorRepository;
        this.schemeRepository = schemeRepository;
        this.holdingRepository = holdingRepository;
        this.folioRepository = folioRepository;
        this.notificationService = notificationService;
        this.auditService = auditService;
        this.currentUser = currentUser;
        this.mapper = mapper;
    }

    @Transactional
    public TrailCommissionDto compute(ComputeCommissionRequest req) {
        Distributor distributor = distributorRepository.findById(req.distributorId())
                .orElseThrow(() -> ResourceNotFoundException.of("Distributor", req.distributorId()));
        FundScheme scheme = schemeRepository.findById(req.schemeId())
                .orElseThrow(() -> ResourceNotFoundException.of("FundScheme", req.schemeId()));

        BigDecimal aum = Calc.money(holdingRepository
                .sumCurrentValueByDistributorAndScheme(distributor.getId(), scheme.getId()));
        // One billing month of an annualised trail rate on the period-end AUM.
        BigDecimal annual = Calc.percentOf(aum, req.trailRate());
        BigDecimal commission = annual.divide(MONTHS, Calc.AMOUNT_SCALE, Calc.RM);

        TrailCommission tc = commissionRepository
                .findByDistributor_IdAndScheme_IdAndPeriod(distributor.getId(), scheme.getId(), req.period())
                .orElseGet(() -> TrailCommission.builder()
                        .distributor(distributor).scheme(scheme).period(req.period()).build());
        tc.setAumManaged(aum);
        tc.setTrailRate(Calc.rate(req.trailRate()));
        tc.setCommissionAmount(commission);
        tc.setStatus(CommissionStatus.COMPUTED);
        tc = commissionRepository.save(tc);

        auditService.record("COMMISSION_COMPUTE", "TrailCommission", tc.getId(),
                "AUM " + aum + " @ " + req.trailRate() + "% -> " + commission + " for " + req.period());
        return mapper.toCommissionDto(tc);
    }

    @Transactional
    public TrailCommissionDto approve(Long id) {
        TrailCommission tc = require(id);
        if (tc.getStatus() != CommissionStatus.COMPUTED) {
            throw new BusinessException("Only COMPUTED commissions can be approved");
        }
        tc.setStatus(CommissionStatus.APPROVED);
        auditService.record("COMMISSION_APPROVE", "TrailCommission", id, "Approved");
        return mapper.toCommissionDto(commissionRepository.save(tc));
    }

    @Transactional
    public TrailCommissionDto pay(Long id) {
        TrailCommission tc = require(id);
        if (tc.getStatus() != CommissionStatus.APPROVED) {
            throw new BusinessException("Only APPROVED commissions can be paid");
        }
        tc.setStatus(CommissionStatus.PAID);
        tc.setPayoutDate(LocalDate.now());
        commissionRepository.save(tc);
        if (tc.getDistributor().getUser() != null) {
            notificationService.notify(tc.getDistributor().getUser(), NotificationCategory.COMMISSION,
                    "Trail commission of " + tc.getCommissionAmount() + " for " + tc.getPeriod()
                            + " (" + tc.getScheme().getSchemeName() + ") has been paid");
        }
        auditService.record("COMMISSION_PAY", "TrailCommission", id,
                "Paid " + tc.getCommissionAmount());
        return mapper.toCommissionDto(tc);
    }

    @Transactional(readOnly = true)
    public List<TrailCommissionDto> listByDistributor(Long distributorId) {
        return commissionRepository.findByDistributor_IdOrderByPeriodDesc(distributorId)
                .stream().map(mapper::toCommissionDto).toList();
    }

    @Transactional(readOnly = true)
    public List<TrailCommissionDto> listForCurrentDistributor() {
        Distributor distributor = requireForCurrentUser();
        return listByDistributor(distributor.getId());
    }

    @Transactional(readOnly = true)
    public DistributorDashboardDto currentDistributorDashboard() {
        Distributor d = requireForCurrentUser();
        BigDecimal aum = Calc.money(holdingRepository.sumCurrentValueByDistributor(d.getId()));
        int folioCount = folioRepository.findByDistributor_Id(d.getId()).size();
        List<TrailCommission> commissions = commissionRepository.findByDistributor_IdOrderByPeriodDesc(d.getId());
        BigDecimal paid = commissions.stream().filter(c -> c.getStatus() == CommissionStatus.PAID)
                .map(c -> Calc.nz(c.getCommissionAmount())).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal pending = commissions.stream().filter(c -> c.getStatus() != CommissionStatus.PAID)
                .map(c -> Calc.nz(c.getCommissionAmount())).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new DistributorDashboardDto(d.getId(), d.getName(), d.getArnNumber(), folioCount,
                aum, Calc.money(paid), Calc.money(pending));
    }

    private Distributor requireForCurrentUser() {
        return distributorRepository.findByUser_Id(currentUser.getId())
                .orElseThrow(() -> new BusinessException("No distributor profile is linked to your account"));
    }

    private TrailCommission require(Long id) {
        return commissionRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("TrailCommission", id));
    }
}
