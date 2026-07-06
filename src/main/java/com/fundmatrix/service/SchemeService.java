package com.fundmatrix.service;

import com.fundmatrix.common.exception.BusinessException;
import com.fundmatrix.common.exception.ResourceNotFoundException;
import com.fundmatrix.domain.FundScheme;
import com.fundmatrix.domain.SchemeOption;
import com.fundmatrix.domain.enums.OptionStatus;
import com.fundmatrix.domain.enums.SchemeStatus;
import com.fundmatrix.dto.FundSchemeDto;
import com.fundmatrix.dto.SaveOptionRequest;
import com.fundmatrix.dto.SaveSchemeRequest;
import com.fundmatrix.dto.SchemeOptionDto;
import com.fundmatrix.repository.FundSchemeRepository;
import com.fundmatrix.repository.NavRecordRepository;
import com.fundmatrix.repository.SchemeOptionRepository;
import com.fundmatrix.domain.NavRecord;
import com.fundmatrix.security.CurrentUserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Fund scheme catalogue and scheme-option administration. */
@Service
public class SchemeService {

    private final FundSchemeRepository schemeRepository;
    private final SchemeOptionRepository optionRepository;
    private final NavRecordRepository navRepository;
    private final AuditService auditService;
    private final CurrentUserService currentUser;
    private final Mapper mapper;

    public SchemeService(FundSchemeRepository schemeRepository, SchemeOptionRepository optionRepository,
                         NavRecordRepository navRepository, AuditService auditService,
                         CurrentUserService currentUser, Mapper mapper) {
        this.schemeRepository = schemeRepository;
        this.optionRepository = optionRepository;
        this.navRepository = navRepository;
        this.auditService = auditService;
        this.currentUser = currentUser;
        this.mapper = mapper;
    }

    @Transactional
    public FundSchemeDto create(SaveSchemeRequest req) {
        if (schemeRepository.existsBySchemeCodeIgnoreCase(req.schemeCode())) {
            throw new BusinessException("Scheme code " + req.schemeCode() + " already exists");
        }
        FundScheme scheme = new FundScheme();
        apply(scheme, req);
        scheme.setStatus(req.status() != null ? req.status() : SchemeStatus.ACTIVE);
        scheme = schemeRepository.save(scheme);
        auditService.record("SCHEME_CREATE", "FundScheme", scheme.getId(), "Created " + scheme.getSchemeName());
        return toDto(scheme);
    }

    @Transactional
    public FundSchemeDto update(Long id, SaveSchemeRequest req) {
        FundScheme scheme = schemeRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("FundScheme", id));
        if (!scheme.getSchemeCode().equalsIgnoreCase(req.schemeCode())
                && schemeRepository.existsBySchemeCodeIgnoreCase(req.schemeCode())) {
            throw new BusinessException("Scheme code " + req.schemeCode() + " already exists");
        }
        apply(scheme, req);
        if (req.status() != null) {
            scheme.setStatus(req.status());
        }
        auditService.record("SCHEME_UPDATE", "FundScheme", id, "Updated " + scheme.getSchemeName());
        return toDto(schemeRepository.save(scheme));
    }

    @Transactional(readOnly = true)
    public List<FundSchemeDto> list() {
        return schemeRepository.findAll().stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public FundSchemeDto get(Long id) {
        return toDto(schemeRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("FundScheme", id)));
    }

    @Transactional
    public SchemeOptionDto addOption(Long schemeId, SaveOptionRequest req) {
        FundScheme scheme = schemeRepository.findById(schemeId)
                .orElseThrow(() -> ResourceNotFoundException.of("FundScheme", schemeId));
        if (req.isin() != null && !req.isin().isBlank()
                && optionRepository.existsByIsinIgnoreCase(req.isin())) {
            throw new BusinessException("ISIN " + req.isin() + " already exists");
        }
        SchemeOption option = SchemeOption.builder()
                .scheme(scheme)
                .optionType(req.optionType())
                .isin(req.isin())
                .status(req.status() != null ? req.status() : OptionStatus.ACTIVE)
                .build();
        option = optionRepository.save(option);
        auditService.record("OPTION_CREATE", "SchemeOption", option.getId(),
                req.optionType() + " option for scheme " + scheme.getSchemeName());
        return mapper.toOptionDto(option, latestNav(option.getId()));
    }

    @Transactional(readOnly = true)
    public List<SchemeOptionDto> listOptions(Long schemeId) {
        return optionRepository.findByScheme_Id(schemeId).stream()
                .map(o -> mapper.toOptionDto(o, latestNav(o.getId()))).toList();
    }

    private void apply(FundScheme scheme, SaveSchemeRequest req) {
        scheme.setSchemeName(req.schemeName());
        scheme.setSchemeCode(req.schemeCode());
        scheme.setCategory(req.category());
        scheme.setRiskProfile(req.riskProfile());
        scheme.setBenchmarkIndex(req.benchmarkIndex());
        scheme.setFundManagerId(req.fundManagerId());
        scheme.setFundManagerName(req.fundManagerName());
        scheme.setMinInvestment(req.minInvestment());
        scheme.setExitLoadSlab(req.exitLoadSlab());
        scheme.setExitLoadRate(req.exitLoadRate());
        scheme.setExitLoadPeriodDays(req.exitLoadPeriodDays());
        scheme.setExpenseRatio(req.expenseRatio());
        scheme.setMinSipAmount(req.minSipAmount());
        scheme.setMinSwpAmount(req.minSwpAmount());
        scheme.setCutoffTime(req.cutoffTime());
    }

    private FundSchemeDto toDto(FundScheme scheme) {
        List<SchemeOptionDto> options = optionRepository.findByScheme_Id(scheme.getId()).stream()
                .map(o -> mapper.toOptionDto(o, latestNav(o.getId()))).toList();
        return mapper.toSchemeDto(scheme, options);
    }

    private java.math.BigDecimal latestNav(Long optionId) {
        return navRepository.findTopByOption_IdOrderByNavDateDesc(optionId)
                .map(NavRecord::getNavValue).orElse(null);
    }
}
