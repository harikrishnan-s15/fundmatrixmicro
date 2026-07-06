package com.fundmatrix.config;

import com.fundmatrix.common.Calc;
import com.fundmatrix.domain.Allotment;
import com.fundmatrix.domain.Distributor;
import com.fundmatrix.domain.DividendDeclaration;
import com.fundmatrix.domain.FolioHolding;
import com.fundmatrix.domain.FundScheme;
import com.fundmatrix.domain.InvestorFolio;
import com.fundmatrix.domain.KycRecord;
import com.fundmatrix.domain.NavRecord;
import com.fundmatrix.domain.SchemeOption;
import com.fundmatrix.domain.SipMandate;
import com.fundmatrix.domain.Transaction;
import com.fundmatrix.domain.TrailCommission;
import com.fundmatrix.domain.User;
import com.fundmatrix.domain.enums.AllotmentStatus;
import com.fundmatrix.domain.enums.CommissionModel;
import com.fundmatrix.domain.enums.CommissionStatus;
import com.fundmatrix.domain.enums.CutOffStatus;
import com.fundmatrix.domain.enums.DistributorStatus;
import com.fundmatrix.domain.enums.DividendStatus;
import com.fundmatrix.domain.enums.FolioStatus;
import com.fundmatrix.domain.enums.KycStatus;
import com.fundmatrix.domain.enums.KycType;
import com.fundmatrix.domain.enums.ModeOfHolding;
import com.fundmatrix.domain.enums.NavStatus;
import com.fundmatrix.domain.enums.OptionStatus;
import com.fundmatrix.domain.enums.OptionType;
import com.fundmatrix.domain.enums.RiskProfile;
import com.fundmatrix.domain.enums.Role;
import com.fundmatrix.domain.enums.SchemeCategory;
import com.fundmatrix.domain.enums.SchemeStatus;
import com.fundmatrix.domain.enums.SipFrequency;
import com.fundmatrix.domain.enums.SipStatus;
import com.fundmatrix.domain.enums.TaxStatus;
import com.fundmatrix.domain.enums.TransactionStatus;
import com.fundmatrix.domain.enums.TransactionType;
import com.fundmatrix.domain.enums.UserStatus;
import com.fundmatrix.repository.AllotmentRepository;
import com.fundmatrix.repository.DistributorRepository;
import com.fundmatrix.repository.DividendDeclarationRepository;
import com.fundmatrix.repository.FolioHoldingRepository;
import com.fundmatrix.repository.FundSchemeRepository;
import com.fundmatrix.repository.InvestorFolioRepository;
import com.fundmatrix.repository.KycRecordRepository;
import com.fundmatrix.repository.NavRecordRepository;
import com.fundmatrix.repository.SchemeOptionRepository;
import com.fundmatrix.repository.SipMandateRepository;
import com.fundmatrix.repository.TrailCommissionRepository;
import com.fundmatrix.repository.TransactionRepository;
import com.fundmatrix.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.ApplicationArguments;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;

/**
 * Seeds a representative dataset on first start (empty DB) so every portal has meaningful
 * content: users for all roles, distributors, a multi-category scheme catalogue with
 * published NAVs, investor folios with holdings and transactions, a pending ops queue,
 * an active SIP, a declared dividend and a computed trail commission.
 */
@Component
@ConditionalOnProperty(name = "fundmatrix.seed.enabled", havingValue = "true", matchIfMissing = true)
public class DataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);
    private static final String DEMO_PASSWORD = "Password@123";

    private final UserRepository userRepository;
    private final DistributorRepository distributorRepository;
    private final FundSchemeRepository schemeRepository;
    private final SchemeOptionRepository optionRepository;
    private final NavRecordRepository navRepository;
    private final InvestorFolioRepository folioRepository;
    private final KycRecordRepository kycRepository;
    private final FolioHoldingRepository holdingRepository;
    private final TransactionRepository transactionRepository;
    private final AllotmentRepository allotmentRepository;
    private final SipMandateRepository sipRepository;
    private final DividendDeclarationRepository dividendRepository;
    private final TrailCommissionRepository commissionRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(UserRepository userRepository, DistributorRepository distributorRepository,
                      FundSchemeRepository schemeRepository, SchemeOptionRepository optionRepository,
                      NavRecordRepository navRepository, InvestorFolioRepository folioRepository,
                      KycRecordRepository kycRepository, FolioHoldingRepository holdingRepository,
                      TransactionRepository transactionRepository, AllotmentRepository allotmentRepository,
                      SipMandateRepository sipRepository, DividendDeclarationRepository dividendRepository,
                      TrailCommissionRepository commissionRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.distributorRepository = distributorRepository;
        this.schemeRepository = schemeRepository;
        this.optionRepository = optionRepository;
        this.navRepository = navRepository;
        this.folioRepository = folioRepository;
        this.kycRepository = kycRepository;
        this.holdingRepository = holdingRepository;
        this.transactionRepository = transactionRepository;
        this.allotmentRepository = allotmentRepository;
        this.sipRepository = sipRepository;
        this.dividendRepository = dividendRepository;
        this.commissionRepository = commissionRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userRepository.count() > 0) {
            log.info("FundMatrix: data already present, skipping seed.");
            return;
        }
        log.info("FundMatrix: seeding demo dataset...");

        // ---- Staff & admin users ----
        user("System Administrator", "admin@fundmatrix.io", "9000000001", Role.ADMIN);
        user("Olivia Operations", "ops@fundmatrix.io", "9000000002", Role.FUND_OPS);
        user("Aakash Accountant", "accountant@fundmatrix.io", "9000000003", Role.FUND_ACCOUNTANT);
        user("Chitra Compliance", "compliance@fundmatrix.io", "9000000004", Role.COMPLIANCE);

        // ---- Distributors + their login users ----
        User arnoldUser = user("Arnold (WealthBridge)", "arnold@wealthbridge.io", "9100000001", Role.DISTRIBUTOR);
        User meeraUser = user("Meera (FinSmart)", "meera@finsmart.io", "9100000002", Role.DISTRIBUTOR);
        Distributor wealthBridge = distributor("WealthBridge Advisors", "ARN-123456", "E123456",
                CommissionModel.TRAIL, arnoldUser);
        Distributor finSmart = distributor("FinSmart Capital", "ARN-789012", "E789012",
                CommissionModel.BOTH, meeraUser);

        // ---- Investors ----
        User ravi = user("Ravi Kumar", "ravi@example.com", "9200000001", Role.INVESTOR);
        User priya = user("Priya Sharma", "priya@example.com", "9200000002", Role.INVESTOR);
        User sanjay = user("Sanjay Mehta", "sanjay@example.com", "9200000003", Role.INVESTOR);

        kyc(ravi, KycType.FULL, "PAN", "ABCDE1234F", KycStatus.COMPLIANT, LocalDate.now().minusYears(1));
        kyc(priya, KycType.EKYC, "Aadhaar-eKYC", "XXXX-1234", KycStatus.COMPLIANT, LocalDate.now().minusMonths(6));
        kyc(sanjay, KycType.SIMPLIFIED, "PAN", "PQRSX9876L", KycStatus.PENDING, null);

        // ---- Scheme catalogue ----
        FundScheme bluechip = scheme("FundMatrix Bluechip Equity Fund", "FM-BLU-EQ", SchemeCategory.EQUITY,
                RiskProfile.HIGH, "NIFTY 50 TRI", "Anita Rao", "5000", "1.8000",
                "1% if redeemed within 365 days", "1.0000", 365);
        SchemeOption bluechipGrowth = option(bluechip, OptionType.GROWTH, "INF000A01011");
        SchemeOption bluechipPayout = option(bluechip, OptionType.DIVIDEND_PAYOUT, "INF000A01029");
        nav(bluechipGrowth, "78.4500");
        nav(bluechipPayout, "23.1000");

        FundScheme corpBond = scheme("FundMatrix Corporate Bond Fund", "FM-CORP-DEBT", SchemeCategory.DEBT,
                RiskProfile.MODERATE, "CRISIL Corporate Bond Index", "Vikram Singh", "5000", "0.9000",
                "Nil", null, null);
        SchemeOption bondGrowth = option(corpBond, OptionType.GROWTH, "INF000A02019");
        nav(bondGrowth, "34.2000");

        FundScheme balanced = scheme("FundMatrix Balanced Advantage Fund", "FM-BAL-HYB", SchemeCategory.HYBRID,
                RiskProfile.MODERATE, "CRISIL Hybrid 35+65", "Anita Rao", "5000", "1.4000",
                "1% if redeemed within 180 days", "1.0000", 180);
        SchemeOption balancedGrowth = option(balanced, OptionType.GROWTH, "INF000A03017");
        SchemeOption balancedReinvest = option(balanced, OptionType.DIVIDEND_REINVESTMENT, "INF000A03025");
        nav(balancedGrowth, "45.6700");
        nav(balancedReinvest, "18.3400");

        FundScheme liquid = scheme("FundMatrix Liquid Fund", "FM-LIQ", SchemeCategory.LIQUID,
                RiskProfile.LOW, "CRISIL Liquid Fund Index", "Vikram Singh", "1000", "0.2000",
                "Nil", null, null);
        SchemeOption liquidGrowth = option(liquid, OptionType.GROWTH, "INF000A04015");
        nav(liquidGrowth, "3650.1234");

        FundScheme elss = scheme("FundMatrix Tax Saver ELSS", "FM-ELSS", SchemeCategory.ELSS,
                RiskProfile.HIGH, "NIFTY 500 TRI", "Anita Rao", "500", "1.6000",
                "Nil (3-year lock-in)", null, null);
        SchemeOption elssGrowth = option(elss, OptionType.GROWTH, "INF000A05013");
        nav(elssGrowth, "56.7800");

        // ---- Folios ----
        InvestorFolio raviFolio = folio(ravi, wealthBridge, TaxStatus.INDIVIDUAL, ModeOfHolding.SINGLE,
                "Sunita Kumar (Spouse) - 100%", "HDFC-XXXX1234");
        InvestorFolio priyaFolio = folio(priya, wealthBridge, TaxStatus.INDIVIDUAL, ModeOfHolding.ANYONE_OR_SURVIVOR,
                "Arjun Sharma (Son) - 100%", "ICICI-XXXX5678");
        InvestorFolio sanjayFolio = folio(sanjay, finSmart, TaxStatus.HUF, ModeOfHolding.SINGLE,
                "Reena Mehta (Spouse) - 100%", "SBI-XXXX9012");

        // ---- Allotted holdings (with backing transactions) ----
        allotHolding(raviFolio, bluechipGrowth, "100000", "78.4500", 30);
        allotHolding(raviFolio, bondGrowth, "50000", "34.2000", 25);
        allotHolding(priyaFolio, balancedGrowth, "75000", "45.6700", 20);
        allotHolding(priyaFolio, balancedReinvest, "40000", "18.3400", 18);
        allotHolding(priyaFolio, elssGrowth, "25000", "56.7800", 15);
        allotHolding(sanjayFolio, liquidGrowth, "200000", "3650.1234", 10);

        // ---- Pending queue for Fund Operations ----
        pendingSubscription(raviFolio, elssGrowth, "15000");
        pendingRedemption(sanjayFolio, liquidGrowth, "10.0000");

        // ---- Active SIP ----
        sip(raviFolio, bluechipGrowth, "5000", SipFrequency.MONTHLY, 36, 4);

        // ---- Declared dividend (awaiting entitlement computation) ----
        DividendDeclaration dividend = new DividendDeclaration();
        dividend.setScheme(balanced);
        dividend.setOption(balancedReinvest);
        dividend.setRecordDate(LocalDate.now().minusDays(2));
        dividend.setDividendPerUnit(new BigDecimal("0.7500"));
        dividend.setTotalDistributionAmount(BigDecimal.ZERO);
        dividend.setStatus(DividendStatus.DECLARED);
        dividendRepository.save(dividend);

        // ---- Computed trail commission for the prior month ----
        BigDecimal wbBluechipAum = holdingRepository
                .sumCurrentValueByDistributorAndScheme(wealthBridge.getId(), bluechip.getId());
        trailCommission(wealthBridge, bluechip, YearMonth.now().minusMonths(1).toString(),
                Calc.money(wbBluechipAum), "0.7500");

        // ---- Backfill NAV AUM/units totals from holdings ----
        backfillNavTotals();

        log.info("FundMatrix: seed complete. Demo login password for all accounts: {}", DEMO_PASSWORD);
    }

    // ----------------------------------------------------------------- builders

    private User user(String name, String email, String phone, Role role) {
        return userRepository.save(User.builder()
                .name(name).email(email).phone(phone).role(role).status(UserStatus.ACTIVE)
                .password(passwordEncoder.encode(DEMO_PASSWORD)).build());
    }

    private Distributor distributor(String name, String arn, String euin, CommissionModel model, User user) {
        return distributorRepository.save(Distributor.builder()
                .name(name).arnNumber(arn).euinNumber(euin).empanelmentDate(LocalDate.now().minusYears(2))
                .commissionModel(model).status(DistributorStatus.ACTIVE).user(user).build());
    }

    private void kyc(User investor, KycType type, String docType, String docRef, KycStatus status, LocalDate verified) {
        kycRepository.save(KycRecord.builder()
                .investor(investor).kycType(type).documentType(docType).documentRef(docRef)
                .verifiedDate(verified).kycStatus(status).build());
    }

    private FundScheme scheme(String name, String code, SchemeCategory category, RiskProfile risk,
                              String benchmark, String manager, String minInv, String expense,
                              String exitSlab, String exitRate, Integer exitDays) {
        return schemeRepository.save(FundScheme.builder()
                .schemeName(name).schemeCode(code).category(category).riskProfile(risk)
                .benchmarkIndex(benchmark).fundManagerName(manager)
                .minInvestment(new BigDecimal(minInv)).expenseRatio(new BigDecimal(expense))
                .exitLoadSlab(exitSlab)
                .exitLoadRate(exitRate != null ? new BigDecimal(exitRate) : null)
                .exitLoadPeriodDays(exitDays)
                .status(SchemeStatus.ACTIVE).build());
    }

    private SchemeOption option(FundScheme scheme, OptionType type, String isin) {
        return optionRepository.save(SchemeOption.builder()
                .scheme(scheme).optionType(type).isin(isin).status(OptionStatus.ACTIVE).build());
    }

    private void nav(SchemeOption option, String value) {
        navRepository.save(NavRecord.builder()
                .scheme(option.getScheme()).option(option).navDate(LocalDate.now())
                .navValue(new BigDecimal(value)).status(NavStatus.PUBLISHED).build());
    }

    private InvestorFolio folio(User investor, Distributor distributor, TaxStatus tax, ModeOfHolding mode,
                                String nominee, String bankRef) {
        InvestorFolio folio = folioRepository.save(InvestorFolio.builder()
                .folioNumber("TMP-" + investor.getId() + "-" + System.nanoTime())
                .investor(investor).distributor(distributor).taxStatus(tax).modeOfHolding(mode)
                .nomineeDetails(nominee).bankAccountRef(bankRef).status(FolioStatus.ACTIVE).build());
        folio.setFolioNumber(String.format("FOL%05d", folio.getId()));
        return folioRepository.save(folio);
    }

    private void allotHolding(InvestorFolio folio, SchemeOption option, String amountStr, String navStr,
                              int daysAgo) {
        BigDecimal amount = new BigDecimal(amountStr);
        BigDecimal nav = new BigDecimal(navStr);
        BigDecimal units = Calc.unitsFor(amount, nav);
        BigDecimal value = Calc.amountFor(units, nav);

        holdingRepository.save(FolioHolding.builder()
                .folio(folio).scheme(option.getScheme()).option(option)
                .unitsHeld(units).averageCostNav(nav).currentValue(value)
                .unrealisedGainLoss(Calc.money(value.subtract(units.multiply(nav))))
                .lastUpdated(Instant.now()).build());

        Transaction txn = transactionRepository.save(baseTxn(folio, option, TransactionType.SUBSCRIPTION, daysAgo)
                .amount(Calc.money(amount)).units(units).applicableNav(nav)
                .status(TransactionStatus.ALLOTTED).build());
        txn.setTransactionRef(String.format("TXN%08d", txn.getId()));
        transactionRepository.save(txn);

        allotmentRepository.save(Allotment.builder()
                .transaction(txn).unitsAllotted(units).allotmentNav(nav)
                .allotmentDate(LocalDate.now().minusDays(daysAgo)).status(AllotmentStatus.ALLOTTED).build());
    }

    private void pendingSubscription(InvestorFolio folio, SchemeOption option, String amountStr) {
        Transaction txn = transactionRepository.save(baseTxn(folio, option, TransactionType.SUBSCRIPTION, 0)
                .amount(new BigDecimal(amountStr)).status(TransactionStatus.RECEIVED).build());
        txn.setTransactionRef(String.format("TXN%08d", txn.getId()));
        transactionRepository.save(txn);
    }

    private void pendingRedemption(InvestorFolio folio, SchemeOption option, String unitsStr) {
        Transaction txn = transactionRepository.save(baseTxn(folio, option, TransactionType.REDEMPTION, 0)
                .units(new BigDecimal(unitsStr)).status(TransactionStatus.RECEIVED).build());
        txn.setTransactionRef(String.format("TXN%08d", txn.getId()));
        transactionRepository.save(txn);
    }

    private Transaction.TransactionBuilder baseTxn(InvestorFolio folio, SchemeOption option,
                                                   TransactionType type, int daysAgo) {
        return Transaction.builder()
                .transactionRef("TMP-" + System.nanoTime())
                .folio(folio).scheme(option.getScheme()).option(option)
                .transactionType(type)
                .transactionDate(Instant.now().minus(daysAgo, ChronoUnit.DAYS))
                .cutOffStatus(CutOffStatus.BEFORE_CUTOFF)
                .status(TransactionStatus.RECEIVED);
    }

    private void sip(InvestorFolio folio, SchemeOption option, String amount, SipFrequency freq,
                     int count, int executed) {
        SipMandate mandate = sipRepository.save(SipMandate.builder()
                .mandateRef("TMP-" + System.nanoTime())
                .folio(folio).scheme(option.getScheme()).option(option)
                .amount(new BigDecimal(amount)).frequency(freq)
                .startDate(LocalDate.now().minusMonths(executed)).endDate(LocalDate.now().plusMonths(count - executed))
                .instalmentCount(count).instalmentsExecuted(executed)
                .nextInstalmentDate(LocalDate.now().plusDays(5)).status(SipStatus.ACTIVE).build());
        mandate.setMandateRef(String.format("SIP%06d", mandate.getId()));
        sipRepository.save(mandate);
    }

    private void trailCommission(Distributor distributor, FundScheme scheme, String period,
                                 BigDecimal aum, String trailRate) {
        BigDecimal annual = Calc.percentOf(aum, new BigDecimal(trailRate));
        BigDecimal monthly = annual.divide(BigDecimal.valueOf(12), Calc.AMOUNT_SCALE, Calc.RM);
        commissionRepository.save(TrailCommission.builder()
                .distributor(distributor).scheme(scheme).period(period)
                .aumManaged(aum).trailRate(new BigDecimal(trailRate)).commissionAmount(monthly)
                .status(CommissionStatus.COMPUTED).build());
    }

    private void backfillNavTotals() {
        for (NavRecord navRecord : navRepository.findAll()) {
            BigDecimal units = holdingRepository.findByOption_Id(navRecord.getOption().getId()).stream()
                    .map(h -> Calc.nz(h.getUnitsHeld())).reduce(BigDecimal.ZERO, BigDecimal::add);
            navRecord.setTotalUnitsOutstanding(Calc.units(units));
            navRecord.setTotalAum(Calc.amountFor(units, navRecord.getNavValue()));
            navRepository.save(navRecord);
        }
    }
}
