package com.fundmatrix.service;

import com.fundmatrix.common.Calc;
import com.fundmatrix.domain.FolioHolding;
import com.fundmatrix.domain.enums.KycStatus;
import com.fundmatrix.domain.enums.SchemeStatus;
import com.fundmatrix.domain.enums.SipStatus;
import com.fundmatrix.domain.enums.TransactionStatus;
import com.fundmatrix.dto.AdminStatsDto;
import com.fundmatrix.dto.ComplianceSummaryDto;
import com.fundmatrix.dto.FolioHoldingDto;
import com.fundmatrix.dto.InvestorDashboardDto;
import com.fundmatrix.repository.DistributorRepository;
import com.fundmatrix.repository.FolioHoldingRepository;
import com.fundmatrix.repository.FundSchemeRepository;
import com.fundmatrix.repository.InvestorFolioRepository;
import com.fundmatrix.repository.KycRecordRepository;
import com.fundmatrix.repository.NotificationRepository;
import com.fundmatrix.repository.SipMandateRepository;
import com.fundmatrix.repository.TransactionRepository;
import com.fundmatrix.repository.UserRepository;
import com.fundmatrix.domain.enums.NotificationStatus;
import com.fundmatrix.security.CurrentUserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/** Aggregated, role-specific dashboard read models. */
@Service
public class DashboardService {

    private final InvestorFolioRepository folioRepository;
    private final FolioHoldingRepository holdingRepository;
    private final SipMandateRepository sipRepository;
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final FundSchemeRepository schemeRepository;
    private final DistributorRepository distributorRepository;
    private final TransactionRepository transactionRepository;
    private final KycRecordRepository kycRepository;
    private final TransactionService transactionService;
    private final HoldingService holdingService;
    private final CurrentUserService currentUser;
    private final Mapper mapper;

    public DashboardService(InvestorFolioRepository folioRepository, FolioHoldingRepository holdingRepository,
                            SipMandateRepository sipRepository, NotificationRepository notificationRepository,
                            UserRepository userRepository, FundSchemeRepository schemeRepository,
                            DistributorRepository distributorRepository, TransactionRepository transactionRepository,
                            KycRecordRepository kycRepository, TransactionService transactionService,
                            HoldingService holdingService, CurrentUserService currentUser, Mapper mapper) {
        this.folioRepository = folioRepository;
        this.holdingRepository = holdingRepository;
        this.sipRepository = sipRepository;
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.schemeRepository = schemeRepository;
        this.distributorRepository = distributorRepository;
        this.transactionRepository = transactionRepository;
        this.kycRepository = kycRepository;
        this.transactionService = transactionService;
        this.holdingService = holdingService;
        this.currentUser = currentUser;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public InvestorDashboardDto investorDashboard() {
        Long investorId = currentUser.getId();
        int folioCount = folioRepository.findByInvestor_Id(investorId).size();
        List<FolioHolding> holdings = holdingRepository.findByFolio_Investor_Id(investorId);

        BigDecimal invested = BigDecimal.ZERO;
        BigDecimal currentValue = BigDecimal.ZERO;
        BigDecimal unrealised = BigDecimal.ZERO;
        for (FolioHolding h : holdings) {
            invested = invested.add(Calc.nz(h.getUnitsHeld()).multiply(Calc.nz(h.getAverageCostNav())));
            currentValue = currentValue.add(Calc.nz(h.getCurrentValue()));
            unrealised = unrealised.add(Calc.nz(h.getUnrealisedGainLoss()));
        }
        int activeSips = (int) sipRepository.findByFolio_Investor_Id(investorId).stream()
                .filter(s -> s.getStatus() == SipStatus.ACTIVE).count();
        long unread = notificationRepository.countByUser_IdAndStatus(investorId, NotificationStatus.UNREAD);

        List<FolioHoldingDto> holdingDtos = holdings.stream()
                .map(h -> mapper.toHoldingDto(h, holdingService.latestNavOrNull(h.getOption().getId())))
                .toList();

        return new InvestorDashboardDto(folioCount, Calc.money(invested), Calc.money(currentValue),
                Calc.money(unrealised), activeSips, unread, holdingDtos);
    }

    @Transactional(readOnly = true)
    public AdminStatsDto adminStats() {
        long totalUsers = userRepository.count();
        long totalSchemes = schemeRepository.count();
        long activeSchemes = schemeRepository.findByStatus(SchemeStatus.ACTIVE).size();
        long totalFolios = folioRepository.count();
        long totalDistributors = distributorRepository.count();
        long pending = transactionRepository.findByStatusInOrderByTransactionDateAsc(
                List.of(TransactionStatus.RECEIVED, TransactionStatus.ACCEPTED)).size();
        BigDecimal totalAum = holdingRepository.findAll().stream()
                .map(h -> Calc.nz(h.getCurrentValue())).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new AdminStatsDto(totalUsers, totalSchemes, activeSchemes, totalFolios,
                totalDistributors, pending, Calc.money(totalAum));
    }

    @Transactional(readOnly = true)
    public ComplianceSummaryDto complianceSummary() {
        long compliant = kycRepository.findByKycStatus(KycStatus.COMPLIANT).size();
        long nonCompliant = kycRepository.findByKycStatus(KycStatus.NON_COMPLIANT).size();
        long pending = kycRepository.findByKycStatus(KycStatus.PENDING).size();
        long expired = kycRepository.findByKycStatus(KycStatus.EXPIRED).size();
        var flagged = transactionService.flaggedTransactions();
        return new ComplianceSummaryDto(compliant, nonCompliant, pending, expired,
                flagged.size(), flagged);
    }
}
