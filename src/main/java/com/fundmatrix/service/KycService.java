package com.fundmatrix.service;

import com.fundmatrix.common.exception.BusinessException;
import com.fundmatrix.common.exception.ResourceNotFoundException;
import com.fundmatrix.domain.KycRecord;
import com.fundmatrix.domain.User;
import com.fundmatrix.domain.enums.KycStatus;
import com.fundmatrix.domain.enums.Role;
import com.fundmatrix.dto.KycRecordDto;
import com.fundmatrix.dto.SubmitKycRequest;
import com.fundmatrix.repository.KycRecordRepository;
import com.fundmatrix.repository.UserRepository;
import com.fundmatrix.domain.enums.NotificationCategory;
import com.fundmatrix.security.CurrentUserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/** KYC record capture and verification workflow. */
@Service
public class KycService {

    private final KycRecordRepository kycRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final AuditService auditService;
    private final CurrentUserService currentUser;
    private final Mapper mapper;

    public KycService(KycRecordRepository kycRepository, UserRepository userRepository,
                      NotificationService notificationService, AuditService auditService,
                      CurrentUserService currentUser, Mapper mapper) {
        this.kycRepository = kycRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.auditService = auditService;
        this.currentUser = currentUser;
        this.mapper = mapper;
    }

    /**
     * Investor self-submission: the investor uploads their own KYC details for verification.
     * The investor is the authenticated user — no one can submit KYC on another's behalf.
     * The record is created in PENDING state and awaits Fund Ops / Compliance verification.
     */
    @Transactional
    public KycRecordDto submit(SubmitKycRequest req) {
        User investor = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> ResourceNotFoundException.of("User", currentUser.getId()));
        if (investor.getRole() != Role.INVESTOR) {
            throw new BusinessException("Only investors can submit KYC");
        }
        KycRecord record = KycRecord.builder()
                .investor(investor)
                .kycType(req.kycType())
                .documentType(req.documentType())
                .documentRef(req.documentRef())
                .kycStatus(KycStatus.PENDING)
                .build();
        record = kycRepository.save(record);
        auditService.record("KYC_SUBMIT", "KycRecord", record.getId(),
                "KYC submitted by investor " + investor.getName());
        notificationService.notify(investor, NotificationCategory.KYC,
                "Your KYC has been submitted and is pending verification");
        return mapper.toKycDto(record);
    }

    @Transactional(readOnly = true)
    public List<KycRecordDto> list(KycStatus status) {
        List<KycRecord> records = (status == null)
                ? kycRepository.findAll() : kycRepository.findByKycStatus(status);
        return records.stream().map(mapper::toKycDto).toList();
    }

    @Transactional(readOnly = true)
    public List<KycRecordDto> listForInvestor(Long investorId) {
        return kycRepository.findByInvestor_Id(investorId).stream().map(mapper::toKycDto).toList();
    }

    @Transactional(readOnly = true)
    public List<KycRecordDto> mine() {
        return listForInvestor(currentUser.getId());
    }

    @Transactional
    public KycRecordDto updateStatus(Long id, KycStatus status) {
        KycRecord record = kycRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("KycRecord", id));
        record.setKycStatus(status);
        if (status == KycStatus.COMPLIANT) {
            record.setVerifiedDate(LocalDate.now());
        }
        kycRepository.save(record);
        auditService.record("KYC_STATUS", "KycRecord", id, "KYC status set to " + status);
        notificationService.notify(record.getInvestor(), NotificationCategory.KYC,
                "Your KYC status is now " + status);
        return mapper.toKycDto(record);
    }
}
