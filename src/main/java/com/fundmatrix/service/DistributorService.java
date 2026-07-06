package com.fundmatrix.service;

import com.fundmatrix.common.Calc;
import com.fundmatrix.common.exception.BusinessException;
import com.fundmatrix.common.exception.ResourceNotFoundException;
import com.fundmatrix.domain.Distributor;
import com.fundmatrix.domain.User;
import com.fundmatrix.domain.enums.DistributorStatus;
import com.fundmatrix.domain.enums.Role;
import com.fundmatrix.dto.DistributorDto;
import com.fundmatrix.dto.SaveDistributorRequest;
import com.fundmatrix.repository.DistributorRepository;
import com.fundmatrix.repository.FolioHoldingRepository;
import com.fundmatrix.repository.UserRepository;
import com.fundmatrix.security.CurrentUserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Distributor empanelment and lookup, with AUM enrichment. */
@Service
public class DistributorService {

    private final DistributorRepository distributorRepository;
    private final UserRepository userRepository;
    private final FolioHoldingRepository holdingRepository;
    private final AuditService auditService;
    private final CurrentUserService currentUser;
    private final Mapper mapper;

    public DistributorService(DistributorRepository distributorRepository, UserRepository userRepository,
                              FolioHoldingRepository holdingRepository, AuditService auditService,
                              CurrentUserService currentUser, Mapper mapper) {
        this.distributorRepository = distributorRepository;
        this.userRepository = userRepository;
        this.holdingRepository = holdingRepository;
        this.auditService = auditService;
        this.currentUser = currentUser;
        this.mapper = mapper;
    }

    @Transactional
    public DistributorDto create(SaveDistributorRequest req) {
        if (req.arnNumber() != null && !req.arnNumber().isBlank()
                && distributorRepository.existsByArnNumberIgnoreCase(req.arnNumber())) {
            throw new BusinessException("A distributor with ARN " + req.arnNumber() + " already exists");
        }
        Distributor d = new Distributor();
        apply(d, req);
        d.setStatus(req.status() != null ? req.status() : DistributorStatus.ACTIVE);
        d = distributorRepository.save(d);
        auditService.record("DISTRIBUTOR_CREATE", "Distributor", d.getId(), "Empanelled " + d.getName());
        return toDto(d);
    }

    @Transactional
    public DistributorDto update(Long id, SaveDistributorRequest req) {
        Distributor d = distributorRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Distributor", id));
        apply(d, req);
        if (req.status() != null) {
            d.setStatus(req.status());
        }
        auditService.record("DISTRIBUTOR_UPDATE", "Distributor", id, "Updated " + d.getName());
        return toDto(distributorRepository.save(d));
    }

    @Transactional(readOnly = true)
    public List<DistributorDto> list() {
        return distributorRepository.findAll().stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public DistributorDto get(Long id) {
        return toDto(distributorRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Distributor", id)));
    }

    /** Resolves the distributor record linked to the authenticated distributor user. */
    @Transactional(readOnly = true)
    public Distributor requireForCurrentUser() {
        return distributorRepository.findByUser_Id(currentUser.getId())
                .orElseThrow(() -> new BusinessException(
                        "No distributor profile is linked to your account"));
    }

    private void apply(Distributor d, SaveDistributorRequest req) {
        d.setName(req.name());
        d.setArnNumber(req.arnNumber());
        d.setEuinNumber(req.euinNumber());
        d.setEmpanelmentDate(req.empanelmentDate());
        d.setCommissionModel(req.commissionModel());
        if (req.userId() != null) {
            User user = userRepository.findById(req.userId())
                    .orElseThrow(() -> ResourceNotFoundException.of("User", req.userId()));
            if (user.getRole() != Role.DISTRIBUTOR) {
                throw new BusinessException("Linked user must have the DISTRIBUTOR role");
            }
            d.setUser(user);
        }
    }

    private DistributorDto toDto(Distributor d) {
        return mapper.toDistributorDto(d, Calc.money(holdingRepository.sumCurrentValueByDistributor(d.getId())));
    }
}
