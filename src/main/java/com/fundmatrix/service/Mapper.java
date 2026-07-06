package com.fundmatrix.service;

import com.fundmatrix.domain.Allotment;
import com.fundmatrix.domain.Distributor;
import com.fundmatrix.domain.DividendDeclaration;
import com.fundmatrix.domain.FolioHolding;
import com.fundmatrix.domain.FundExpenseAccrual;
import com.fundmatrix.domain.FundScheme;
import com.fundmatrix.domain.InvestorDividendEntitlement;
import com.fundmatrix.domain.InvestorFolio;
import com.fundmatrix.domain.KycRecord;
import com.fundmatrix.domain.NavRecord;
import com.fundmatrix.domain.Notification;
import com.fundmatrix.domain.SchemeOption;
import com.fundmatrix.domain.SipMandate;
import com.fundmatrix.domain.SwpMandate;
import com.fundmatrix.domain.Transaction;
import com.fundmatrix.domain.TransactionFlag;
import com.fundmatrix.domain.TrailCommission;
import com.fundmatrix.domain.User;
import com.fundmatrix.dto.AllotmentDto;
import com.fundmatrix.dto.DistributorDto;
import com.fundmatrix.dto.DividendDeclarationDto;
import com.fundmatrix.dto.EntitlementDto;
import com.fundmatrix.dto.ExpenseAccrualDto;
import com.fundmatrix.dto.FolioDto;
import com.fundmatrix.dto.FolioHoldingDto;
import com.fundmatrix.dto.FundSchemeDto;
import com.fundmatrix.dto.KycRecordDto;
import com.fundmatrix.dto.NavRecordDto;
import com.fundmatrix.dto.NotificationDto;
import com.fundmatrix.dto.SchemeOptionDto;
import com.fundmatrix.dto.SipMandateDto;
import com.fundmatrix.dto.SwpMandateDto;
import com.fundmatrix.dto.TrailCommissionDto;
import com.fundmatrix.dto.TransactionDto;
import com.fundmatrix.dto.TransactionFlagDto;
import com.fundmatrix.dto.UserDto;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;


@Component
public class Mapper {

    public UserDto toUserDto(User u) {
        return new UserDto(u.getId(), u.getName(), u.getEmail(), u.getPhone(),
                u.getRole(), u.getStatus(), u.getCreatedAt());
    }

    public SchemeOptionDto toOptionDto(SchemeOption o, BigDecimal latestNav) {
        return new SchemeOptionDto(o.getId(), o.getScheme().getId(), o.getScheme().getSchemeName(),
                o.getOptionType(), o.getIsin(), o.getStatus(), latestNav);
    }

    public FundSchemeDto toSchemeDto(FundScheme s, List<SchemeOptionDto> options) {
        return new FundSchemeDto(s.getId(), s.getSchemeName(), s.getSchemeCode(), s.getCategory(),
                s.getRiskProfile(), s.getBenchmarkIndex(), s.getFundManagerId(), s.getFundManagerName(),
                s.getMinInvestment(), s.getExitLoadSlab(), s.getExitLoadRate(), s.getExitLoadPeriodDays(),
                s.getExpenseRatio(), s.getMinSipAmount(), s.getMinSwpAmount(), s.getCutoffTime(),
                s.getStatus(), options);
    }

    public FolioDto toFolioDto(InvestorFolio f, BigDecimal currentValue) {
        Distributor d = f.getDistributor();
        return new FolioDto(f.getId(), f.getFolioNumber(), f.getInvestor().getId(),
                f.getInvestor().getName(), d != null ? d.getId() : null, d != null ? d.getName() : null,
                f.getTaxStatus(), f.getModeOfHolding(), f.getNomineeDetails(), f.getBankAccountRef(),
                f.getStatus(), currentValue);
    }

    public FolioHoldingDto toHoldingDto(FolioHolding h, BigDecimal latestNav) {
        return new FolioHoldingDto(h.getId(), h.getFolio().getId(), h.getFolio().getFolioNumber(),
                h.getScheme().getId(), h.getScheme().getSchemeName(), h.getOption().getId(),
                h.getOption().getOptionType().name(), h.getUnitsHeld(), h.getAverageCostNav(),
                latestNav, h.getCurrentValue(), h.getUnrealisedGainLoss(), h.getLastUpdated());
    }

    public KycRecordDto toKycDto(KycRecord k) {
        return new KycRecordDto(k.getId(), k.getInvestor().getId(), k.getInvestor().getName(),
                k.getKycType(), k.getDocumentType(), k.getDocumentRef(), k.getVerifiedDate(),
                k.getKycStatus());
    }

    public TransactionDto toTxnDto(Transaction t) {
        return new TransactionDto(t.getId(), t.getTransactionRef(), t.getFolio().getId(),
                t.getFolio().getFolioNumber(), t.getScheme().getId(), t.getScheme().getSchemeName(),
                t.getOption().getId(), t.getOption().getOptionType().name(), t.getTransactionType(),
                t.getAmount(), t.getUnits(), t.getApplicableNav(), t.getTransactionDate(),
                t.getCutOffStatus(), t.getStatus(), t.getExitLoadAmount(), t.getRemarks());
    }

    public AllotmentDto toAllotmentDto(Allotment a) {
        return new AllotmentDto(a.getId(), a.getTransaction().getId(),
                a.getTransaction().getTransactionRef(), a.getUnitsAllotted(), a.getAllotmentNav(),
                a.getAllotmentDate(), a.getStatus());
    }

    public SipMandateDto toSipDto(SipMandate s) {
        return new SipMandateDto(s.getId(), s.getMandateRef(), s.getFolio().getId(),
                s.getFolio().getFolioNumber(), s.getScheme().getId(), s.getScheme().getSchemeName(),
                s.getOption().getId(), s.getAmount(), s.getFrequency(), s.getStartDate(), s.getEndDate(),
                s.getInstalmentCount(), s.getInstalmentsExecuted(), s.getNextInstalmentDate(), s.getStatus());
    }

    public SwpMandateDto toSwpDto(SwpMandate s) {
        return new SwpMandateDto(s.getId(), s.getMandateRef(), s.getFolio().getId(),
                s.getFolio().getFolioNumber(), s.getScheme().getId(), s.getScheme().getSchemeName(),
                s.getOption().getId(), s.getAmount(), s.getFrequency(), s.getStartDate(), s.getEndDate(),
                s.getInstalmentCount(), s.getInstalmentsExecuted(), s.getNextInstalmentDate(), s.getStatus());
    }

    public TransactionFlagDto toFlagDto(TransactionFlag f) {
        Transaction t = f.getTransaction();
        return new TransactionFlagDto(f.getId(), t.getId(), t.getTransactionRef(),
                t.getFolio().getFolioNumber(), t.getScheme().getSchemeName(), f.getAmount(),
                f.getReason(), f.getStatus(), f.getReviewNote(), f.getCreatedDate(), f.getReviewedDate());
    }

    public NavRecordDto toNavDto(NavRecord n) {
        return new NavRecordDto(n.getId(), n.getScheme().getId(), n.getScheme().getSchemeName(),
                n.getOption().getId(), n.getOption().getOptionType().name(), n.getNavDate(),
                n.getNavValue(), n.getTotalAum(), n.getTotalUnitsOutstanding(), n.getPublishedById(),
                n.getStatus());
    }

    public ExpenseAccrualDto toAccrualDto(FundExpenseAccrual a) {
        return new ExpenseAccrualDto(a.getId(), a.getScheme().getId(), a.getScheme().getSchemeName(),
                a.getExpenseType(), a.getAccrualAmount(), a.getAccrualDate(), a.getAnnualisedRate(),
                a.getStatus(), a.getReversalReason());
    }

    public DividendDeclarationDto toDividendDto(DividendDeclaration d, long entitlementCount) {
        return new DividendDeclarationDto(d.getId(), d.getScheme().getId(), d.getScheme().getSchemeName(),
                d.getOption().getId(), d.getOption().getOptionType().name(), d.getRecordDate(),
                d.getDividendPerUnit(), d.getTotalDistributionAmount(), d.getDeclaredById(), d.getStatus(),
                entitlementCount);
    }

    public EntitlementDto toEntitlementDto(InvestorDividendEntitlement e) {
        return new EntitlementDto(e.getId(), e.getDeclaration().getId(), e.getFolio().getId(),
                e.getFolio().getFolioNumber(), e.getFolio().getInvestor().getName(),
                e.getUnitsOnRecordDate(), e.getGrossDividend(), e.getTaxDeducted(), e.getNetDividend(),
                e.getPayoutMode(), e.getStatus());
    }

    public DistributorDto toDistributorDto(Distributor d, BigDecimal aum) {
        return new DistributorDto(d.getId(), d.getName(), d.getArnNumber(), d.getEuinNumber(),
                d.getEmpanelmentDate(), d.getCommissionModel(), d.getStatus(),
                d.getUser() != null ? d.getUser().getId() : null, aum);
    }

    public TrailCommissionDto toCommissionDto(TrailCommission c) {
        return new TrailCommissionDto(c.getId(), c.getDistributor().getId(), c.getDistributor().getName(),
                c.getScheme().getId(), c.getScheme().getSchemeName(), c.getPeriod(), c.getAumManaged(),
                c.getTrailRate(), c.getCommissionAmount(), c.getPayoutDate(), c.getStatus());
    }

    public NotificationDto toNotificationDto(Notification n) {
        return new NotificationDto(n.getId(), n.getMessage(), n.getCategory(), n.getStatus(),
                n.getCreatedDate());
    }
}
