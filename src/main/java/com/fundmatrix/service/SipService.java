package com.fundmatrix.service;

import com.fundmatrix.common.exception.BusinessException;
import com.fundmatrix.common.exception.ResourceNotFoundException;
import com.fundmatrix.domain.InvestorFolio;
import com.fundmatrix.domain.SchemeOption;
import com.fundmatrix.domain.SipMandate;
import com.fundmatrix.domain.enums.FolioStatus;
import com.fundmatrix.domain.enums.NotificationCategory;
import com.fundmatrix.domain.enums.Role;
import com.fundmatrix.domain.enums.SipFrequency;
import com.fundmatrix.domain.enums.SipStatus;
import com.fundmatrix.dto.CreateSipRequest;
import com.fundmatrix.dto.SipMandateDto;
import com.fundmatrix.repository.SchemeOptionRepository;
import com.fundmatrix.repository.SipMandateRepository;
import com.fundmatrix.security.CurrentUserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/** Systematic Investment Plan mandate management and instalment execution. */
@Service
public class SipService {

    private final SipMandateRepository sipRepository;
    private final SchemeOptionRepository optionRepository;
    private final FolioService folioService;
    private final TransactionService transactionService;
    private final NotificationService notificationService;
    private final AuditService auditService;
    private final CurrentUserService currentUser;
    private final Mapper mapper;

    public SipService(SipMandateRepository sipRepository, SchemeOptionRepository optionRepository,
                      FolioService folioService, TransactionService transactionService,
                      NotificationService notificationService, AuditService auditService,
                      CurrentUserService currentUser, Mapper mapper) {
        this.sipRepository = sipRepository;
        this.optionRepository = optionRepository;
        this.folioService = folioService;
        this.transactionService = transactionService;
        this.notificationService = notificationService;
        this.auditService = auditService;
        this.currentUser = currentUser;
        this.mapper = mapper;
    }

    @Transactional
    public SipMandateDto create(CreateSipRequest req) {
        InvestorFolio folio = folioService.loadAccessible(req.folioId());
        if (folio.getStatus() != FolioStatus.ACTIVE) {
            throw new BusinessException("Folio is not active");
        }
        SchemeOption option = optionRepository.findById(req.optionId())
                .orElseThrow(() -> ResourceNotFoundException.of("SchemeOption", req.optionId()));

        var minSip = option.getScheme().getMinSipAmount();
        if (minSip != null && req.amount().compareTo(minSip) < 0) {
            throw new BusinessException("Minimum SIP amount for " + option.getScheme().getSchemeName()
                    + " is " + minSip);
        }

        SipMandate mandate = SipMandate.builder()
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
        mandate = sipRepository.save(mandate);
        mandate.setMandateRef(String.format("SIP%06d", mandate.getId()));
        mandate = sipRepository.save(mandate);

        notificationService.notify(folio.getInvestor(), NotificationCategory.SIP,
                "SIP mandate " + mandate.getMandateRef() + " for " + req.amount() + " ("
                        + req.frequency() + ") registered");
        auditService.record("SIP_CREATE", "SipMandate", mandate.getId(),
                "SIP " + req.amount() + " " + req.frequency() + " folio " + folio.getFolioNumber());
        return mapper.toSipDto(mandate);
    }

    @Transactional(readOnly = true)
    public List<SipMandateDto> listForCurrentUser() {
        List<SipMandate> mandates = (currentUser.getRole() == Role.INVESTOR)
                ? sipRepository.findByFolio_Investor_Id(currentUser.getId())
                : sipRepository.findAll();
        return mandates.stream().map(mapper::toSipDto).toList();
    }

    @Transactional(readOnly = true)
    public List<SipMandateDto> dueMandates() {
        return sipRepository.findByStatus(SipStatus.ACTIVE).stream()
                .filter(m -> m.getNextInstalmentDate() != null
                        && !m.getNextInstalmentDate().isAfter(LocalDate.now()))
                .map(mapper::toSipDto).toList();
    }

    /** Executes one SIP instalment: places and allots a subscription, then advances the schedule. */
    @Transactional
    public SipMandateDto runInstalment(Long id) {
        SipMandate mandate = sipRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("SipMandate", id));
        if (mandate.getStatus() != SipStatus.ACTIVE) {
            throw new BusinessException("SIP mandate is " + mandate.getStatus() + "; cannot run instalment");
        }

        transactionService.placeAndAllotSipInstalment(mandate);

        int executed = mandate.getInstalmentsExecuted() + 1;
        mandate.setInstalmentsExecuted(executed);
        mandate.setNextInstalmentDate(nextDate(mandate));
        if (mandate.getInstalmentCount() != null && executed >= mandate.getInstalmentCount()) {
            mandate.setStatus(SipStatus.COMPLETED);
        } else if (mandate.getEndDate() != null && mandate.getNextInstalmentDate().isAfter(mandate.getEndDate())) {
            mandate.setStatus(SipStatus.COMPLETED);
        }
        mandate = sipRepository.save(mandate);

        notificationService.notify(mandate.getFolio().getInvestor(), NotificationCategory.SIP,
                "SIP instalment " + executed + " for " + mandate.getMandateRef() + " processed");
        auditService.record("SIP_INSTALMENT", "SipMandate", mandate.getId(),
                "Instalment " + executed + " executed");
        return mapper.toSipDto(mandate);
    }

    @Transactional
    public SipMandateDto changeStatus(Long id, SipStatus status) {
        SipMandate mandate = sipRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("SipMandate", id));
        mandate.setStatus(status);
        auditService.record("SIP_STATUS", "SipMandate", id, "Status set to " + status);
        return mapper.toSipDto(sipRepository.save(mandate));
    }

    private LocalDate nextDate(SipMandate mandate) {
        LocalDate base = mandate.getNextInstalmentDate() != null
                ? mandate.getNextInstalmentDate() : LocalDate.now();
        return mandate.getFrequency() == SipFrequency.QUARTERLY ? base.plusMonths(3) : base.plusMonths(1);
    }
}
