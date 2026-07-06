package com.fundmatrix.service;

import com.fundmatrix.common.Calc;
import com.fundmatrix.common.exception.BusinessException;
import com.fundmatrix.common.exception.ResourceNotFoundException;
import com.fundmatrix.domain.Allotment;
import com.fundmatrix.domain.FolioHolding;
import com.fundmatrix.domain.FundScheme;
import com.fundmatrix.domain.InvestorFolio;
import com.fundmatrix.domain.SchemeOption;
import com.fundmatrix.domain.SipMandate;
import com.fundmatrix.domain.SwpMandate;
import com.fundmatrix.domain.Transaction;
import com.fundmatrix.domain.TransactionFlag;
import com.fundmatrix.domain.enums.AllotmentStatus;
import com.fundmatrix.domain.enums.CutOffStatus;
import com.fundmatrix.domain.enums.FlagStatus;
import com.fundmatrix.domain.enums.FolioStatus;
import com.fundmatrix.domain.enums.KycStatus;
import com.fundmatrix.domain.enums.NotificationCategory;
import com.fundmatrix.domain.enums.OptionStatus;
import com.fundmatrix.domain.enums.Role;
import com.fundmatrix.domain.enums.SchemeCategory;
import com.fundmatrix.domain.enums.SchemeStatus;
import com.fundmatrix.domain.enums.TransactionStatus;
import com.fundmatrix.domain.enums.TransactionType;
import com.fundmatrix.dto.AllotmentDto;
import com.fundmatrix.dto.AllotmentResultDto;
import com.fundmatrix.dto.RedemptionRequest;
import com.fundmatrix.dto.SubscriptionRequest;
import com.fundmatrix.dto.SwitchRequest;
import com.fundmatrix.dto.TransactionDto;
import com.fundmatrix.repository.AllotmentRepository;
import com.fundmatrix.repository.FolioHoldingRepository;
import com.fundmatrix.repository.KycRecordRepository;
import com.fundmatrix.repository.SchemeOptionRepository;
import com.fundmatrix.repository.TransactionFlagRepository;
import com.fundmatrix.repository.TransactionRepository;
import com.fundmatrix.security.CurrentUserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Transaction processing: subscription, redemption, switch and SIP instalment flows,
 * with cut-off enforcement, the operations accept→allot workflow, NAV-based unit
 * allotment, exit-load handling and holding updates.
 */
@Service
public class TransactionService {

    /** Transactions at or above this value are surfaced for compliance review. */
    private static final BigDecimal LARGE_TXN_THRESHOLD = new BigDecimal("1000000");

    private final TransactionRepository transactionRepository;
    private final AllotmentRepository allotmentRepository;
    private final SchemeOptionRepository optionRepository;
    private final FolioHoldingRepository holdingRepository;
    private final KycRecordRepository kycRepository;
    private final TransactionFlagRepository flagRepository;
    private final FolioService folioService;
    private final HoldingService holdingService;
    private final NotificationService notificationService;
    private final AuditService auditService;
    private final CurrentUserService currentUser;
    private final Mapper mapper;
    private final TransactionService self;   // self-proxy: per-item transactions in batch allotment

    private final LocalTime standardCutoff;
    private final LocalTime liquidCutoff;

    public TransactionService(TransactionRepository transactionRepository,
                              AllotmentRepository allotmentRepository,
                              SchemeOptionRepository optionRepository,
                              FolioHoldingRepository holdingRepository, KycRecordRepository kycRepository,
                              TransactionFlagRepository flagRepository,
                              FolioService folioService,
                              HoldingService holdingService, NotificationService notificationService,
                              AuditService auditService, CurrentUserService currentUser, Mapper mapper,
                              @Lazy TransactionService self,
                              @Value("${fundmatrix.operations.cutoff-time}") String cutoffTime,
                              @Value("${fundmatrix.operations.liquid-cutoff-time}") String liquidCutoffTime) {
        this.transactionRepository = transactionRepository;
        this.allotmentRepository = allotmentRepository;
        this.optionRepository = optionRepository;
        this.holdingRepository = holdingRepository;
        this.kycRepository = kycRepository;
        this.flagRepository = flagRepository;
        this.folioService = folioService;
        this.holdingService = holdingService;
        this.notificationService = notificationService;
        this.auditService = auditService;
        this.currentUser = currentUser;
        this.mapper = mapper;
        this.self = self;
        this.standardCutoff = LocalTime.parse(cutoffTime);
        this.liquidCutoff = LocalTime.parse(liquidCutoffTime);
    }

    // ----------------------------------------------------------------- placement

    @Transactional
    public TransactionDto placeSubscription(SubscriptionRequest req) {
        InvestorFolio folio = activeFolio(req.folioId());
        SchemeOption option = activeOption(req.optionId());
        FundScheme scheme = option.getScheme();

        BigDecimal amount = Calc.money(req.amount());
        boolean firstPurchase = holdingRepository
                .findByFolio_IdAndOption_Id(folio.getId(), option.getId()).isEmpty();
        if (firstPurchase && scheme.getMinInvestment() != null
                && amount.compareTo(scheme.getMinInvestment()) < 0) {
            throw new BusinessException("Minimum investment for " + scheme.getSchemeName()
                    + " is " + scheme.getMinInvestment());
        }

        Transaction txn = newTransaction(folio, option, TransactionType.SUBSCRIPTION);
        txn.setAmount(amount);
        txn.setStatus(TransactionStatus.RECEIVED);
        txn = save(txn);
        flagIfLarge(txn);   // flag large subscriptions immediately at placement

        notifyInvestor(folio, NotificationCategory.TRANSACTION,
                "Subscription of " + amount + " into " + scheme.getSchemeName() + " received ("
                        + txn.getTransactionRef() + ")");
        auditService.record("SUBSCRIPTION_PLACE", "Transaction", txn.getId(),
                "Subscription " + amount + " folio " + folio.getFolioNumber());
        return mapper.toTxnDto(txn);
    }

    @Transactional
    public TransactionDto placeRedemption(RedemptionRequest req) {
        InvestorFolio folio = activeFolio(req.folioId());
        SchemeOption option = activeOption(req.optionId());
        FolioHolding holding = holdingRepository.findByFolio_IdAndOption_Id(folio.getId(), option.getId())
                .orElseThrow(() -> new BusinessException("No holding to redeem in this option"));

        BigDecimal units = resolveRedemptionUnits(req, option, holding);
        if (units.signum() <= 0) {
            throw new BusinessException("Redemption units must be greater than zero");
        }
        if (units.compareTo(Calc.nz(holding.getUnitsHeld())) > 0) {
            throw new BusinessException("Insufficient units: holding " + holding.getUnitsHeld());
        }

        Transaction txn = newTransaction(folio, option, TransactionType.REDEMPTION);
        txn.setUnits(Calc.units(units));
        txn.setStatus(TransactionStatus.RECEIVED);
        txn = save(txn);

        notifyInvestor(folio, NotificationCategory.TRANSACTION,
                "Redemption of " + txn.getUnits() + " units from "
                        + option.getScheme().getSchemeName() + " received (" + txn.getTransactionRef() + ")");
        auditService.record("REDEMPTION_PLACE", "Transaction", txn.getId(),
                "Redemption " + txn.getUnits() + " units folio " + folio.getFolioNumber());
        return mapper.toTxnDto(txn);
    }

    @Transactional
    public List<TransactionDto> switchUnits(SwitchRequest req) {
        InvestorFolio folio = activeFolio(req.folioId());
        SchemeOption fromOption = activeOption(req.fromOptionId());
        SchemeOption toOption = activeOption(req.toOptionId());
        if (fromOption.getId().equals(toOption.getId())) {
            throw new BusinessException("Source and target options must differ");
        }
        FolioHolding holding = holdingRepository
                .findByFolio_IdAndOption_Id(folio.getId(), fromOption.getId())
                .orElseThrow(() -> new BusinessException("No holding to switch in the source option"));

        BigDecimal units = req.switchAll() ? Calc.nz(holding.getUnitsHeld()) : Calc.units(req.units());
        if (units == null || units.signum() <= 0 || units.compareTo(Calc.nz(holding.getUnitsHeld())) > 0) {
            throw new BusinessException("Invalid switch units; holding is " + holding.getUnitsHeld());
        }

        BigDecimal fromNav = holdingService.requirePublishedNav(fromOption.getId());
        BigDecimal toNav = holdingService.requirePublishedNav(toOption.getId());
        BigDecimal grossAmount = Calc.amountFor(units, fromNav);

        // Switch-out leg
        holdingService.debitUnits(folio, fromOption, units, fromNav);
        Transaction out = newTransaction(folio, fromOption, TransactionType.SWITCH);
        out.setUnits(Calc.units(units));
        out.setApplicableNav(fromNav);
        out.setAmount(grossAmount);
        out.setStatus(TransactionStatus.ALLOTTED);
        out.setRemarks("Switch-out to " + toOption.getScheme().getSchemeName());
        out = save(out);
        recordAllotment(out, units, fromNav);

        // Switch-in leg
        BigDecimal toUnits = Calc.unitsFor(grossAmount, toNav);
        holdingService.creditUnits(folio, toOption.getScheme(), toOption, toUnits, grossAmount, toNav);
        Transaction in = newTransaction(folio, toOption, TransactionType.SWITCH);
        in.setUnits(toUnits);
        in.setApplicableNav(toNav);
        in.setAmount(grossAmount);
        in.setStatus(TransactionStatus.ALLOTTED);
        in.setRemarks("Switch-in from " + fromOption.getScheme().getSchemeName());
        in = save(in);
        recordAllotment(in, toUnits, toNav);

        notifyInvestor(folio, NotificationCategory.TRANSACTION,
                "Switch of " + units + " units from " + fromOption.getScheme().getSchemeName()
                        + " to " + toOption.getScheme().getSchemeName() + " completed");
        auditService.record("SWITCH", "Transaction", in.getId(),
                "Switch " + units + " units folio " + folio.getFolioNumber());
        return List.of(mapper.toTxnDto(out), mapper.toTxnDto(in));
    }

    // ----------------------------------------------------------------- ops workflow

    @Transactional
    public TransactionDto accept(Long id) {
        Transaction txn = require(id);
        if (txn.getStatus() != TransactionStatus.RECEIVED) {
            throw new BusinessException("Only RECEIVED transactions can be accepted");
        }
        txn.setStatus(TransactionStatus.ACCEPTED);
        auditService.record("TXN_ACCEPT", "Transaction", id, "Accepted " + txn.getTransactionRef());
        return mapper.toTxnDto(transactionRepository.save(txn));
    }

    @Transactional
    public TransactionDto allot(Long id) {
        Transaction txn = require(id);
        if (txn.getStatus() != TransactionStatus.ACCEPTED && txn.getStatus() != TransactionStatus.RECEIVED) {
            throw new BusinessException("Only RECEIVED/ACCEPTED transactions can be allotted");
        }
        return switch (txn.getTransactionType()) {
            case SUBSCRIPTION, SIP_INSTALMENT -> allotSubscription(txn);
            case REDEMPTION, SWP_INSTALMENT -> allotRedemption(txn);
            default -> throw new BusinessException(
                    "Transaction type " + txn.getTransactionType() + " is not allotted via this workflow");
        };
    }

    @Transactional
    public TransactionDto reject(Long id, String reason) {
        Transaction txn = require(id);
        if (txn.getStatus() == TransactionStatus.ALLOTTED || txn.getStatus() == TransactionStatus.REVERSED) {
            throw new BusinessException("Allotted/reversed transactions cannot be rejected");
        }
        txn.setStatus(TransactionStatus.REJECTED);
        txn.setRemarks(reason);
        notifyInvestor(txn.getFolio(), NotificationCategory.TRANSACTION,
                "Transaction " + txn.getTransactionRef() + " was rejected: " + reason);
        auditService.record("TXN_REJECT", "Transaction", id, "Rejected: " + reason);
        return mapper.toTxnDto(transactionRepository.save(txn));
    }

    /** Allots each transaction in its OWN transaction (via self-proxy); one failure never aborts the rest. */
    public List<AllotmentResultDto> allotBatch(List<Long> ids) {
        List<AllotmentResultDto> results = new ArrayList<>();
        for (Long id : ids) {
            try {
                TransactionDto dto = self.allot(id);
                results.add(new AllotmentResultDto(id, dto.transactionRef(), true, dto.status().name(),
                        "Allotted " + dto.units() + " units @ NAV " + dto.applicableNav()));
            } catch (Exception ex) {
                String ref = transactionRepository.findById(id)
                        .map(Transaction::getTransactionRef).orElse(String.valueOf(id));
                results.add(new AllotmentResultDto(id, ref, false, "FAILED", ex.getMessage()));
            }
        }
        auditService.record("ALLOT_BATCH", "Transaction", null,
                "Batch allotment attempted for " + ids.size() + " transaction(s)");
        return results;
    }

    private TransactionDto allotSubscription(Transaction txn) {
        SchemeOption option = txn.getOption();
        BigDecimal nav = holdingService.requirePublishedNav(option.getId());
        BigDecimal units = Calc.unitsFor(txn.getAmount(), nav);

        holdingService.creditUnits(txn.getFolio(), txn.getScheme(), option, units, txn.getAmount(), nav);
        txn.setUnits(units);
        txn.setApplicableNav(nav);
        txn.setStatus(TransactionStatus.ALLOTTED);
        transactionRepository.save(txn);
        recordAllotment(txn, units, nav);

        notifyInvestor(txn.getFolio(), NotificationCategory.TRANSACTION,
                units + " units allotted at NAV " + nav + " for " + txn.getTransactionRef());
        auditService.record("ALLOT_SUBSCRIPTION", "Transaction", txn.getId(),
                units + " units @ " + nav);
        return mapper.toTxnDto(txn);
    }

    private TransactionDto allotRedemption(Transaction txn) {
        SchemeOption option = txn.getOption();
        FundScheme scheme = txn.getScheme();
        BigDecimal nav = holdingService.requirePublishedNav(option.getId());
        BigDecimal units = txn.getUnits();
        BigDecimal gross = Calc.amountFor(units, nav);
        BigDecimal exitLoad = computeExitLoad(scheme, gross);
        BigDecimal net = Calc.money(gross.subtract(exitLoad));

        holdingService.debitUnits(txn.getFolio(), option, units, nav);
        txn.setApplicableNav(nav);
        txn.setAmount(net);
        txn.setExitLoadAmount(exitLoad);
        txn.setStatus(TransactionStatus.ALLOTTED);
        transactionRepository.save(txn);
        recordAllotment(txn, units, nav);

        notifyInvestor(txn.getFolio(), NotificationCategory.TRANSACTION,
                "Redemption " + txn.getTransactionRef() + " processed: net payout " + net
                        + (exitLoad.signum() > 0 ? " (exit load " + exitLoad + ")" : ""));
        auditService.record("ALLOT_REDEMPTION", "Transaction", txn.getId(),
                "Redeemed " + units + " units @ " + nav + ", net " + net);
        return mapper.toTxnDto(txn);
    }

    // ----------------------------------------------------------------- SIP support

    /** Creates and immediately allots a SIP instalment subscription for a mandate. */
    @Transactional
    public Transaction placeAndAllotSipInstalment(SipMandate mandate) {
        InvestorFolio folio = mandate.getFolio();
        SchemeOption option = mandate.getOption();
        Transaction txn = newTransaction(folio, option, TransactionType.SIP_INSTALMENT);
        txn.setAmount(Calc.money(mandate.getAmount()));
        txn.setStatus(TransactionStatus.ACCEPTED);
        txn.setSipMandate(mandate);
        txn.setRemarks("SIP instalment for mandate " + mandate.getMandateRef());
        txn = save(txn);
        allotSubscription(txn);
        return txn;
    }

    /**
     * Creates and immediately allots one SWP (withdrawal) instalment for a mandate.
     * Blocks if the folio is frozen, the investor's KYC is not compliant, or there are
     * insufficient units to cover the fixed withdrawal amount.
     */
    @Transactional
    public Transaction placeAndAllotSwpInstalment(SwpMandate mandate) {
        InvestorFolio folio = mandate.getFolio();
        SchemeOption option = mandate.getOption();

        if (folio.getStatus() != FolioStatus.ACTIVE) {
            throw new BusinessException("Folio " + folio.getFolioNumber() + " is " + folio.getStatus()
                    + "; SWP instalment is blocked");
        }
        if (!kycRepository.existsByInvestor_IdAndKycStatus(folio.getInvestor().getId(), KycStatus.COMPLIANT)) {
            throw new BusinessException("KYC is not verified for " + folio.getInvestor().getName()
                    + "; SWP instalment is blocked");
        }

        FolioHolding holding = holdingRepository.findByFolio_IdAndOption_Id(folio.getId(), option.getId())
                .orElseThrow(() -> new BusinessException("No holding to withdraw from for this option"));
        BigDecimal nav = holdingService.requirePublishedNav(option.getId());
        BigDecimal units = Calc.unitsFor(mandate.getAmount(), nav);
        if (units.signum() <= 0) {
            throw new BusinessException("SWP amount is too small to redeem any units");
        }
        if (units.compareTo(Calc.nz(holding.getUnitsHeld())) > 0) {
            throw new BusinessException("Insufficient units for SWP instalment; holding is "
                    + holding.getUnitsHeld());
        }

        Transaction txn = newTransaction(folio, option, TransactionType.SWP_INSTALMENT);
        txn.setUnits(Calc.units(units));
        txn.setStatus(TransactionStatus.ACCEPTED);
        txn.setRemarks("SWP instalment for mandate " + mandate.getMandateRef());
        txn = save(txn);
        allotRedemption(txn);
        return txn;
    }

    // ----------------------------------------------------------------- queries

    @Transactional(readOnly = true)
    public List<TransactionDto> queue() {
        return transactionRepository.findByStatusInOrderByTransactionDateAsc(
                        List.of(TransactionStatus.RECEIVED, TransactionStatus.ACCEPTED))
                .stream().map(mapper::toTxnDto).toList();
    }

    @Transactional(readOnly = true)
    public List<TransactionDto> listForCurrentUser() {
        Role role = currentUser.getRole();
        return switch (role) {
            case INVESTOR -> transactionRepository
                    .findByFolio_Investor_IdOrderByTransactionDateDesc(currentUser.getId())
                    .stream().map(mapper::toTxnDto).toList();
            case DISTRIBUTOR -> transactionRepository.findAll().stream()
                    .filter(t -> t.getFolio().getDistributor() != null
                            && isCurrentDistributorFolio(t.getFolio()))
                    .map(mapper::toTxnDto).toList();
            default -> transactionRepository.findAll().stream().map(mapper::toTxnDto).toList();
        };
    }

    @Transactional(readOnly = true)
    public List<TransactionDto> listByFolio(Long folioId) {
        folioService.loadAccessible(folioId);
        return transactionRepository.findByFolio_IdOrderByTransactionDateDesc(folioId)
                .stream().map(mapper::toTxnDto).toList();
    }

    @Transactional(readOnly = true)
    public AllotmentDto getAllotment(Long transactionId) {
        return allotmentRepository.findByTransaction_Id(transactionId).map(mapper::toAllotmentDto)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No allotment for transaction " + transactionId));
    }

    @Transactional(readOnly = true)
    public List<TransactionDto> flaggedTransactions() {
        return transactionRepository.findAll().stream()
                .filter(t -> t.getAmount() != null && t.getAmount().compareTo(LARGE_TXN_THRESHOLD) >= 0)
                .map(mapper::toTxnDto).toList();
    }

    // ----------------------------------------------------------------- helpers

    private Transaction newTransaction(InvestorFolio folio, SchemeOption option, TransactionType type) {
        return Transaction.builder()
                .folio(folio).scheme(option.getScheme()).option(option)
                .transactionType(type)
                .transactionDate(Instant.now())
                .cutOffStatus(cutOff(option.getScheme()))
                .status(TransactionStatus.RECEIVED)
                .build();
    }

    private Transaction save(Transaction txn) {
        boolean isNew = txn.getId() == null;
        txn = transactionRepository.save(txn);
        if (isNew) {
            txn.setTransactionRef(String.format("TXN%08d", txn.getId()));
            txn = transactionRepository.save(txn);
        }
        return txn;
    }

    private void recordAllotment(Transaction txn, BigDecimal units, BigDecimal nav) {
        Allotment allotment = allotmentRepository.findByTransaction_Id(txn.getId())
                .orElseGet(Allotment::new);
        allotment.setTransaction(txn);
        allotment.setUnitsAllotted(Calc.units(units));
        allotment.setAllotmentNav(nav);
        allotment.setAllotmentDate(LocalDate.now());
        allotment.setStatus(AllotmentStatus.ALLOTTED);
        allotmentRepository.save(allotment);
        flagIfLarge(txn);
    }

    /** Auto-raises an OPEN compliance flag for high-value transactions (>= threshold), once per txn. */
    private void flagIfLarge(Transaction txn) {
        if (txn.getId() == null || txn.getAmount() == null
                || txn.getAmount().compareTo(LARGE_TXN_THRESHOLD) < 0
                || flagRepository.existsByTransaction_Id(txn.getId())) {
            return;
        }
        flagRepository.save(TransactionFlag.builder()
                .transaction(txn)
                .reason("High-value transaction (>= " + LARGE_TXN_THRESHOLD + ")")
                .amount(txn.getAmount())
                .status(FlagStatus.OPEN)
                .createdDate(Instant.now())
                .build());
        auditService.record("TXN_FLAGGED", "Transaction", txn.getId(),
                "Auto-flagged high-value transaction " + txn.getAmount());
    }

    private CutOffStatus cutOff(FundScheme scheme) {
        LocalTime cutoff = scheme.getCategory() == SchemeCategory.LIQUID ? liquidCutoff : standardCutoff;
        if (scheme.getCutoffTime() != null && !scheme.getCutoffTime().isBlank()) {
            try {
                cutoff = LocalTime.parse(scheme.getCutoffTime().trim());   // per-scheme override
            } catch (Exception ignored) {
                // fall back to the category default on malformed config
            }
        }
        return LocalTime.now().isAfter(cutoff) ? CutOffStatus.AFTER_CUTOFF : CutOffStatus.BEFORE_CUTOFF;
    }

    private BigDecimal computeExitLoad(FundScheme scheme, BigDecimal gross) {
        if (scheme.getExitLoadRate() == null || scheme.getExitLoadRate().signum() <= 0) {
            return Calc.money(BigDecimal.ZERO);
        }
        // Simplification: lot-level holding periods are not tracked in Phase 1, so the
        // configured exit-load rate is applied to the redemption value.
        return Calc.percentOf(gross, scheme.getExitLoadRate());
    }

    private BigDecimal resolveRedemptionUnits(RedemptionRequest req, SchemeOption option,
                                              FolioHolding holding) {
        if (req.redeemAll()) {
            return Calc.nz(holding.getUnitsHeld());
        }
        if (req.units() != null) {
            return Calc.units(req.units());
        }
        if (req.amount() != null) {
            BigDecimal nav = holdingService.requirePublishedNav(option.getId());
            return Calc.unitsFor(req.amount(), nav);
        }
        throw new BusinessException("Provide units, amount, or set redeemAll for a redemption");
    }

    private InvestorFolio activeFolio(Long folioId) {
        InvestorFolio folio = folioService.loadAccessible(folioId);
        if (folio.getStatus() != FolioStatus.ACTIVE) {
            throw new BusinessException("Folio " + folio.getFolioNumber() + " is " + folio.getStatus()
                    + "; transactions are not permitted");
        }
        // KYC gate: subscriptions, redemptions and switches are only permitted once the
        // folio's investor has a verified (COMPLIANT) KYC record.
        if (!kycRepository.existsByInvestor_IdAndKycStatus(folio.getInvestor().getId(), KycStatus.COMPLIANT)) {
            throw new BusinessException("KYC is not verified for " + folio.getInvestor().getName()
                    + ". Subscriptions, redemptions and switches are blocked until KYC is COMPLIANT.");
        }
        return folio;
    }

    private SchemeOption activeOption(Long optionId) {
        SchemeOption option = optionRepository.findById(optionId)
                .orElseThrow(() -> ResourceNotFoundException.of("SchemeOption", optionId));
        if (option.getStatus() != OptionStatus.ACTIVE) {
            throw new BusinessException("Scheme option is inactive");
        }
        SchemeStatus s = option.getScheme().getStatus();
        if (s == SchemeStatus.CLOSED || s == SchemeStatus.WOUND_UP) {
            throw new BusinessException("Scheme " + option.getScheme().getSchemeName() + " is " + s);
        }
        return option;
    }

    private Transaction require(Long id) {
        return transactionRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Transaction", id));
    }

    private boolean isCurrentDistributorFolio(InvestorFolio folio) {
        return folio.getDistributor() != null && folio.getDistributor().getUser() != null
                && currentUser.getId().equals(folio.getDistributor().getUser().getId());
    }

    private void notifyInvestor(InvestorFolio folio, NotificationCategory category, String message) {
        notificationService.notify(folio.getInvestor(), category, message);
    }
}
