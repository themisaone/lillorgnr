package no.companyfetcher.model;

public record CompanyData(
        String orgNumber,
        String companyName,
        Integer accountingYear,
        Long revenue,
        Long salaryCost,
        Long ebit,
        String status
) {
    public static CompanyData failed(String orgNumber, String reason) {
        return new CompanyData(orgNumber, null, null, null, null, null, "FAILED: " + reason);
    }
}
