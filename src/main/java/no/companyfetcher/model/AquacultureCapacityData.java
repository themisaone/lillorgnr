package no.companyfetcher.model;

public record AquacultureCapacityData(
        String orgNumber,
        String companyName,
        Double totalCapacity,
        String unit,
        int entryCount,
        String status
) {
    public static AquacultureCapacityData failed(String orgNumber, String reason) {
        return new AquacultureCapacityData(orgNumber, null, null, null, 0, "FAILED: " + reason);
    }
}
