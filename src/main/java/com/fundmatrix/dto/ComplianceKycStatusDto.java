package com.fundmatrix.dto;

/** KYC compliance posture across all investors. */
public record ComplianceKycStatusDto(
        long compliant,
        long pending,
        long nonCompliant,
        long expired,
        long total
) {
}
