package com.fundmatrix.service;

import com.fundmatrix.common.Calc;
import com.fundmatrix.common.exception.BusinessException;
import com.fundmatrix.common.exception.ResourceNotFoundException;
import com.fundmatrix.domain.Distributor;
import com.fundmatrix.domain.FolioHolding;
import com.fundmatrix.domain.InvestorFolio;
import com.fundmatrix.domain.User;
import com.fundmatrix.domain.enums.FolioStatus;
import com.fundmatrix.domain.enums.Role;
import com.fundmatrix.dto.CreateFolioRequest;
import com.fundmatrix.dto.FolioDto;
import com.fundmatrix.dto.FolioHoldingDto;
import com.fundmatrix.repository.DistributorRepository;
import com.fundmatrix.repository.FolioHoldingRepository;
import com.fundmatrix.repository.InvestorFolioRepository;
import com.fundmatrix.repository.UserRepository;
import com.fundmatrix.security.CurrentUserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/** Investor folio lifecycle and holdings, with role-scoped visibility. */
@Service
public class FolioService {

    private final InvestorFolioRepository folioRepository;
    private final UserRepository userRepository;
    private final DistributorRepository distributorRepository;
    private final FolioHoldingRepository holdingRepository;
    private final HoldingService holdingService;
    private final AuditService auditService;
    private final CurrentUserService currentUser;
    private final Mapper mapper;

    public FolioService(InvestorFolioRepository folioRepository, UserRepository userRepository,
                        DistributorRepository distributorRepository, FolioHoldingRepository holdingRepository,
                        HoldingService holdingService, AuditService auditService,
                        CurrentUserService currentUser, Mapper mapper) {
        this.folioRepository = folioRepository;
        this.userRepository = userRepository;
        this.distributorRepository = distributorRepository;
        this.holdingRepository = holdingRepository;
        this.holdingService = holdingService;
        this.auditService = auditService;
        this.currentUser = currentUser;
        this.mapper = mapper;
    }

    @Transactional
    public FolioDto create(CreateFolioRequest req) {
        User investor = resolveInvestor(req);
        Distributor distributor = resolveDistributor(req);

        InvestorFolio folio = InvestorFolio.builder()
                .investor(investor)
                .distributor(distributor)
                .taxStatus(req.taxStatus())
                .modeOfHolding(req.modeOfHolding())
                .nomineeDetails(req.nomineeDetails())
                .bankAccountRef(req.bankAccountRef())
                .status(FolioStatus.ACTIVE)
                .build();
        folio = folioRepository.save(folio);
        folio.setFolioNumber(String.format("FOL%05d", folio.getId()));
        folio = folioRepository.save(folio);

        auditService.record("FOLIO_CREATE", "InvestorFolio", folio.getId(),
                "Folio " + folio.getFolioNumber() + " for investor " + investor.getName());
        return toDto(folio);
    }

    @Transactional(readOnly = true)
    public List<FolioDto> listForCurrentUser() {
        Role role = currentUser.getRole();
        List<InvestorFolio> folios = switch (role) {
            case INVESTOR -> folioRepository.findByInvestor_Id(currentUser.getId());
            case DISTRIBUTOR -> distributorRepository.findByUser_Id(currentUser.getId())
                    .map(d -> folioRepository.findByDistributor_Id(d.getId())).orElseGet(List::of);
            default -> folioRepository.findAll();
        };
        return folios.stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public FolioDto get(Long id) {
        return toDto(loadAccessible(id));
    }

    @Transactional(readOnly = true)
    public List<FolioHoldingDto> holdings(Long folioId) {
        loadAccessible(folioId);
        return holdingRepository.findByFolio_Id(folioId).stream()
                .map(h -> mapper.toHoldingDto(h, holdingService.latestNavOrNull(h.getOption().getId())))
                .toList();
    }

    @Transactional
    public FolioDto updateStatus(Long id, FolioStatus status) {
        InvestorFolio folio = folioRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("InvestorFolio", id));
        folio.setStatus(status);
        auditService.record("FOLIO_STATUS", "InvestorFolio", id, "Status set to " + status);
        return toDto(folioRepository.save(folio));
    }

    /** Loads a folio enforcing role-based visibility; hides existence on access denial. */
    @Transactional(readOnly = true)
    public InvestorFolio loadAccessible(Long id) {
        InvestorFolio folio = folioRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("InvestorFolio", id));
        Role role = currentUser.getRole();
        if (role == Role.INVESTOR && !folio.getInvestor().getId().equals(currentUser.getId())) {
            throw ResourceNotFoundException.of("InvestorFolio", id);
        }
        if (role == Role.DISTRIBUTOR) {
            Long distId = distributorRepository.findByUser_Id(currentUser.getId())
                    .map(Distributor::getId).orElse(null);
            boolean owns = folio.getDistributor() != null && folio.getDistributor().getId().equals(distId);
            if (!owns) {
                throw ResourceNotFoundException.of("InvestorFolio", id);
            }
        }
        return folio;
    }

    private User resolveInvestor(CreateFolioRequest req) {
        if (currentUser.getRole() == Role.INVESTOR) {
            return currentUser.requireUser();
        }
        if (req.investorId() == null) {
            throw new BusinessException("investorId is required when creating a folio on behalf of an investor");
        }
        User investor = userRepository.findById(req.investorId())
                .orElseThrow(() -> ResourceNotFoundException.of("User", req.investorId()));
        if (investor.getRole() != Role.INVESTOR) {
            throw new BusinessException("Folios can only be created for users with the INVESTOR role");
        }
        return investor;
    }

    private Distributor resolveDistributor(CreateFolioRequest req) {
        if (req.distributorId() != null) {
            return distributorRepository.findById(req.distributorId())
                    .orElseThrow(() -> ResourceNotFoundException.of("Distributor", req.distributorId()));
        }
        if (currentUser.getRole() == Role.DISTRIBUTOR) {
            return distributorRepository.findByUser_Id(currentUser.getId()).orElse(null);
        }
        return null;
    }

    private FolioDto toDto(InvestorFolio folio) {
        BigDecimal currentValue = holdingRepository.findByFolio_Id(folio.getId()).stream()
                .map(FolioHolding::getCurrentValue).filter(v -> v != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return mapper.toFolioDto(folio, Calc.money(currentValue));
    }
}
