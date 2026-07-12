package no.companyfetcher.model;

public record OrgNrReportData(
        String orgNumber,
        String orgName,
        Integer accountingYear,
        Long revenue,
        Long salaryCost,
        Long ebit,
        String proffStatus,
        Double totalCapacity,
        String unit,
        int entryCount,
        String aquaStatus,
        Long fee
) {
    public CompanyData toCompanyData() {
        return new CompanyData(orgNumber, orgName, accountingYear, revenue, salaryCost, ebit, proffStatus);
    }

    public AquacultureCapacityData toAquacultureData() {
        return new AquacultureCapacityData(orgNumber, orgName, totalCapacity, unit, entryCount, aquaStatus);
    }
}
