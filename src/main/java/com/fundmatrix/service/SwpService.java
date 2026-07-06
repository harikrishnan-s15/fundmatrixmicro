package com.fundmatrix.service;

import com.fundmatrix.common.exception.BusinessException;
import com.fundmatrix.common.exception.ResourceNotFoundException;
import com.fundmatrix.domain.InvestorFolio;
import com.fundmatrix.domain.SchemeOption;
import com.fundmatrix.domain.SwpMandate;
import com.fundmatrix.domain.enums.FolioStatus;
import com.fundmatrix.domain.enums.NotificationCategory;
import com.fundmatrix.domain.enums.Role;
import com.fundmatrix.domain.enums.SipFrequency;
import com.fundmatrix.domain.enums.SipStatus;
import com.fundmatrix.dto.CreateSwpRequest;
import com.fundmatrix.dto.SwpMandateDto;
import com.fundmatrix.dto.UpdateSwpRequest;
import com.fundmatrix.repository.SchemeOptionRepository;
import com.fundmatrix.repository.SwpMandateRepository;
import com.fundmatrix.security.CurrentUserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/** Systematic Withdrawal Plan mandate management and instalment execution. */
@Service
public class SwpService {

    private final SwpMandateRepository swpRepository;
    private final SchemeOptionRepository optionRepository;
    private final FolioService folioService;
    private final TransactionService transactionService;
    private final NotificationService notificationService;
    private final AuditService auditService;
    private final CurrentUserService currentUser;
    private final Mapper mapper;

    public SwpService(SwpMandateRepository swpRepository, SchemeOptionRepository optionRepository,
                      FolioService folioService, TransactionService transactionService,
                      NotificationService notificationService, AuditService auditService,
                      CurrentUserService currentUser, Mapper mapper) {
        this.swpRepository = swpRepository;
        this.optionRepository = optionRepository;
        this.folioService = folioService;
        this.transactionService = transactionService;
        this.notificationService = notificationService;
        this.auditService = auditService;
        this.currentUser = currentUser;
        this.mapper = mapper;
    }

    @Transactional
    public SwpMandateDto create(CreateSwpRequest req) {
        InvestorFolio folio = folioService.loadAccessible(req.folioId());
        if (folio.getStatus() != FolioStatus.ACTIVE) {
            throw new BusinessException("Folio is not active");
        }
        SchemeOption option = optionRepository.findById(req.optionId())
                .orElseThrow(() -> ResourceNotFoundException.of("SchemeOption", req.optionId()));

        var minSwp = option.getScheme().getMinSwpAmount();
        if (minSwp != null && req.amount().compareTo(minSwp) < 0) {
            throw new BusinessException("Minimum SWP amount for " + option.getScheme().getSchemeName()
                    + " is " + minSwp);
        }

        SwpMandate mandate = SwpMandate.builder()
                .folio(folio).scheme(option.getScheme()).option(option)
                .amount(req.amount())
                .frequency(req.frequency())
                .startDate(req.startDate())
                .endDate(req.endDate())
                .instalmentCount(req.instalmentCount())
                .instalmentsExecuted(0)
                .nextInstalmentDate(req.startDate())
                .status(SipStatus.ACTIVE)
                .build();
        mandate = swpRepository.save(mandate);
        mandate.setMandateRef(String.format("SWP%06d", mandate.getId()));
        mandate = swpRepository.save(mandate);

        notificationService.notify(folio.getInvestor(), NotificationCategory.SIP,
                "SWP mandate " + mandate.getMandateRef() + " for " + req.amount() + " ("
                        + req.frequency() + ") registered");
        auditService.record("SWP_CREATE", "SwpMandate", mandate.getId(),
                "SWP " + req.amount() + " " + req.frequency() + " folio " + folio.getFolioNumber());
        return mapper.toSwpDto(mandate);
    }

    @Transactional(readOnly = true)
    public List<SwpMandateDto> listForCurrentUser() {
        List<SwpMandate> mandates = (currentUser.getRole() == Role.INVESTOR)
                ? swpRepository.findByFolio_Investor_Id(currentUser.getId())
                : swpRepository.findAll();
        return mandates.stream().map(mapper::toSwpDto).toList();
    }

    @Transactional(readOnly = true)
    public SwpMandateDto get(Long id) {
        return mapper.toSwpDto(require(id));
    }

    @Transactional
    public SwpMandateDto update(Long id, UpdateSwpRequest req) {
        SwpMandate mandate = require(id);
        if (mandate.getStatus() == SipStatus.COMPLETED || mandate.getStatus() == SipStatus.CANCELLED) {
            throw new BusinessException("Cannot edit a " + mandate.getStatus() + " mandate");
        }
        if (req.amount() != null) mandate.setAmount(req.amount());
        if (req.frequency() != null) mandate.setFrequency(req.frequency());
        if (req.endDate() != null) mandate.setEndDate(req.endDate());
        if (req.instalmentCount() != null) mandate.setInstalmentCount(req.instalmentCount());
        auditService.record("SWP_UPDATE", "SwpMandate", id, "Mandate updated");
        return mapper.toSwpDto(swpRepository.save(mandate));
    }

    @Transactional
    public SwpMandateDto changeStatus(Long id, SipStatus status) {
        SwpMandate mandate = require(id);
        mandate.setStatus(status);
        auditService.record("SWP_STATUS", "SwpMandate", id, "Status set to " + status);
        return mapper.toSwpDto(swpRepository.save(mandate));
    }

    /** Executes one SWP instalment: redeems units worth the fixed amount, then advances the schedule. */
    @Transactional
    public SwpMandateDto process(Long id) {
        SwpMandate mandate = require(id);
        if (mandate.getStatus() != SipStatus.ACTIVE) {
            throw new BusinessException("SWP mandate is " + mandate.getStatus() + "; cannot process instalment");
        }

        transactionService.placeAndAllotSwpInstalment(mandate);

        int executed = mandate.getInstalmentsExecuted() + 1;
        mandate.setInstalmentsExecuted(executed);
        mandate.setNextInstalmentDate(nextDate(mandate));
        if (mandate.getInstalmentCount() != null && executed >= mandate.getInstalmentCount()) {
            mandate.setStatus(SipStatus.COMPLETED);
        } else if (mandate.getEndDate() != null && mandate.getNextInstalmentDate().isAfter(mandate.getEndDate())) {
            mandate.setStatus(SipStatus.COMPLETED);
        }
        mandate = swpRepository.save(mandate);

        notificationService.notify(mandate.getFolio().getInvestor(), NotificationCategory.SIP,
                "SWP instalment " + executed + " for " + mandate.getMandateRef() + " processed");
        auditService.record("SWP_INSTALMENT", "SwpMandate", mandate.getId(),
                "Instalment " + executed + " executed");
        return mapper.toSwpDto(mandate);
    }

    private SwpMandate require(Long id) {
        return swpRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("SwpMandate", id));
    }

    private LocalDate nextDate(SwpMandate mandate) {
        LocalDate base = mandate.getNextInstalmentDate() != null
                ? mandate.getNextInstalmentDate() : LocalDate.now();
        return mandate.getFrequency() == SipFrequency.QUARTERLY ? base.plusMonths(3) : base.plusMonths(1);
    }
}
