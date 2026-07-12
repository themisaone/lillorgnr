package no.companyfetcher.service;

import no.companyfetcher.model.OrgNrReportData;
import no.companyfetcher.model.ReportRunMode;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class OrgNrReportSummaryTest {

    @Test
    void formatsSummaryCounts() {
        String summary = OrgNrReportSummary.format(List.of(
                new OrgNrReportData("1", "A", 2024, 1L, 2L, 3L, "OK", 100.0, "MTB", 2, "OK", 1000L),
                new OrgNrReportData("2", "B", null, null, null, null, "FAILED: X", null, null, 0, "FAILED: NOT_FOUND", 0L),
                new OrgNrReportData("3", "C", 2024, 1L, 2L, 3L, "OK", null, null, 0, "OK: NO_MATCHING_MTB", 0L)
        ), "OrgNrReport.csv");

        assertTrue(summary.contains("Modus: Proff og MTB"));
        assertTrue(summary.contains("Antall linjer: 3"));
        assertTrue(summary.contains("Proff OK: 2"));
        assertTrue(summary.contains("Proff feilet: 1"));
        assertTrue(summary.contains("Aqua funnet: 1"));
        assertTrue(summary.contains("Aqua ikke funnet: 1"));
        assertTrue(summary.contains("Aqua uten treff: 1"));
    }

    @Test
    void formatsProffOnlySummary() {
        String summary = OrgNrReportSummary.format(List.of(
                new OrgNrReportData("1", "A", 2024, 1L, 2L, 3L, "OK", null, null, 0, null, null)
        ), "OrgNrReport.csv", ReportRunMode.PROFF_ONLY);

        assertTrue(summary.contains("Modus: Bare Proff"));
        assertTrue(summary.contains("Proff OK: 1"));
        assertTrue(summary.contains("Aqua funnet") == false);
    }
}
