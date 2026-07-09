package no.companyfetcher.provider;

import no.companyfetcher.model.CompanyData;

import java.util.Map;

public class DummyProvider implements CompanyProvider {

    private static final Map<String, CompanyData> SAMPLE_DATA = Map.of(
            "994613405", new CompanyData("994613405", "Arnøy Laks AS", 2024, 248_204_000L, 25_194_000L, 15_303_000L, "OK"),
            "895366722", new CompanyData("895366722", "Arnøy Laks Slakteri AS", 2024, 180_000_000L, 20_000_000L, 12_000_000L, "OK"),
            "975862801", new CompanyData("975862801", "Example Company AS", 2024, 50_000_000L, null, 5_000_000L, "PARTIAL")
    );

    private final int accountingYear;

    public DummyProvider(int accountingYear) {
        this.accountingYear = accountingYear;
    }

    @Override
    public CompanyData fetch(String orgNumber) {
        String normalized = orgNumber.replaceAll("\\D", "");
        CompanyData sample = SAMPLE_DATA.get(normalized);
        if (sample != null) {
            return new CompanyData(
                    sample.orgNumber(),
                    sample.companyName(),
                    accountingYear,
                    sample.revenue(),
                    sample.salaryCost(),
                    sample.ebit(),
                    sample.status()
            );
        }

        return new CompanyData(
                normalized,
                "Dummy Company " + normalized,
                accountingYear,
                10_000_000L,
                1_000_000L,
                500_000L,
                "OK"
        );
    }
}
