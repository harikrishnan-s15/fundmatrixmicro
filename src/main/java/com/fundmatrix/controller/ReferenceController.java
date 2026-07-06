package com.fundmatrix.controller;

import com.fundmatrix.domain.enums.CommissionModel;
import com.fundmatrix.domain.enums.ExpenseType;
import com.fundmatrix.domain.enums.KycType;
import com.fundmatrix.domain.enums.ModeOfHolding;
import com.fundmatrix.domain.enums.OptionType;
import com.fundmatrix.domain.enums.RiskProfile;
import com.fundmatrix.domain.enums.Role;
import com.fundmatrix.domain.enums.SchemeCategory;
import com.fundmatrix.domain.enums.SipFrequency;
import com.fundmatrix.domain.enums.TaxStatus;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Exposes enum vocabularies so UI dropdowns stay in sync with the backend. */
@RestController
@RequestMapping("/reference")
@Tag(name = "Reference data", description = "Enum vocabularies for UI dropdowns")
public class ReferenceController {

    @GetMapping("/enums")
    public Map<String, List<String>> enums() {
        Map<String, List<String>> out = new LinkedHashMap<>();
        out.put("roles", names(Role.values()));
        out.put("schemeCategories", names(SchemeCategory.values()));
        out.put("riskProfiles", names(RiskProfile.values()));
        out.put("optionTypes", names(OptionType.values()));
        out.put("taxStatuses", names(TaxStatus.values()));
        out.put("modesOfHolding", names(ModeOfHolding.values()));
        out.put("kycTypes", names(KycType.values()));
        out.put("sipFrequencies", names(SipFrequency.values()));
        out.put("expenseTypes", names(ExpenseType.values()));
        out.put("commissionModels", names(CommissionModel.values()));
        return out;
    }

    private List<String> names(Enum<?>[] values) {
        return Arrays.stream(values).map(Enum::name).toList();
    }
}
