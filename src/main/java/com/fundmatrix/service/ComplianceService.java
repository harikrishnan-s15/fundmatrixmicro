package com.fundmatrix.service;

import com.fundmatrix.common.Calc;
import com.fundmatrix.common.exception.BusinessException;
import com.fundmatrix.common.exception.ResourceNotFoundException;
import com.fundmatrix.domain.TransactionFlag;
import com.fundmatrix.domain.enums.FlagStatus;
import com.fundmatrix.domain.enums.KycStatus;
import com.fundmatrix.dto.ComplianceKycStatusDto;
import com.fundmatrix.dto.ComplianceReportDto;
import com.fundmatrix.dto.TransactionFlagDto;
import com.fundmatrix.repository.KycRecordRepository;
import com.fundmatrix.repository.TransactionFlagRepository;
import com.fundmatrix.security.CurrentUserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Compliance monitoring: KYC posture, transaction-flag review workflow and report generation. */
@Service
public class ComplianceService {

    private final KycRecordRepository kycRepository;
    private final TransactionFlagRepository flagRepository;
    private final CurrentUserService currentUser;
    private final AuditService auditService;
    private final Mapper mapper;

    public ComplianceService(KycRecordRepository kycRepository, TransactionFlagRepository flagRepository,
                             CurrentUserService currentUser, AuditService auditService, Mapper mapper) {
        this.kycRepository = kycRepository;
        this.flagRepository = flagRepository;
        this.currentUser = currentUser;
        this.auditService = auditService;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public ComplianceKycStatusDto kycStatus() {
        return new ComplianceKycStatusDto(
                kycRepository.countByKycStatus(KycStatus.COMPLIANT),
                kycRepository.countByKycStatus(KycStatus.PENDING),
                kycRepository.countByKycStatus(KycStatus.NON_COMPLIANT),
                kycRepository.countByKycStatus(KycStatus.EXPIRED),
                kycRepository.count());
    }

    @Transactional(readOnly = true)
    public List<TransactionFlagDto> flags(FlagStatus status) {
        List<TransactionFlag> list = (status == null)
                ? flagRepository.findAllByOrderByCreatedDateDesc()
                : flagRepository.findByStatusOrderByCreatedDateDesc(status);
        return list.stream().map(mapper::toFlagDto).toList();
    }

    /** Workflow: OPEN → REVIEWED → CLEARED / ESCALATED (and ESCALATED → CLEARED). */
    @Transactional
    public TransactionFlagDto reviewFlag(Long id, FlagStatus to, String note) {
        TransactionFlag flag = flagRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("TransactionFlag", id));
        if (!isValidTransition(flag.getStatus(), to)) {
            throw new BusinessException("Cannot move flag from " + flag.getStatus() + " to " + to);
        }
        flag.setStatus(to);
        flag.setReviewNote(note);
        flag.setReviewedById(currentUser.getId());
        flag.setReviewedDate(Instant.now());
        auditService.record("FLAG_REVIEW", "TransactionFlag", id,
                "Flag -> " + to + (note != null && !note.isBlank() ? " (" + note + ")" : ""));
        return mapper.toFlagDto(flagRepository.save(flag));
    }

    @Transactional   // not read-only: it writes an audit record
    public ComplianceReportDto generateReport() {
        List<TransactionFlag> all = flagRepository.findAll();
        BigDecimal total = all.stream().map(f -> Calc.nz(f.getAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        ComplianceReportDto report = new ComplianceReportDto(
                Instant.now(),
                currentUser.requireUser().getName(),
                kycStatus(),
                flagRepository.countByStatus(FlagStatus.OPEN),
                flagRepository.countByStatus(FlagStatus.REVIEWED),
                flagRepository.countByStatus(FlagStatus.CLEARED),
                flagRepository.countByStatus(FlagStatus.ESCALATED),
                all.size(),
                Calc.money(total));
        auditService.record("COMPLIANCE_REPORT", "Report", null, "Generated compliance report");
        return report;
    }

    private boolean isValidTransition(FlagStatus from, FlagStatus to) {
        return switch (from) {
            case OPEN -> to == FlagStatus.REVIEWED;
            case REVIEWED -> to == FlagStatus.CLEARED || to == FlagStatus.ESCALATED;
            case ESCALATED -> to == FlagStatus.CLEARED;
            case CLEARED -> false;
        };
    }
}
