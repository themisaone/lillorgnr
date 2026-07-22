package no.companyfetcher.service;

import no.companyfetcher.model.OrgNrReportData;
import no.companyfetcher.model.ReportRunMode;

import java.util.List;

public final class OrgNrReportSummary {

    private OrgNrReportSummary() {
    }

    public static String format(List<OrgNrReportData> rows, String outputFile) {
        return format(rows, outputFile, ReportRunMode.PROFF_AND_MTB);
    }

    public static String format(List<OrgNrReportData> rows, String outputFile, ReportRunMode mode) {
        int total = rows.size();
        long proffOk = rows.stream().filter(OrgNrReportSummary::isProffOk).count();
        long proffFailed = rows.stream().filter(OrgNrReportSummary::isProffFailed).count();

        if (mode == ReportRunMode.PROFF_ONLY) {
            return """
                    --- Oppsummering ---
                    
                    Modus: Bare Proff
                    Antall linjer: %d
                    Proff OK: %d
                    Proff feilet: %d
                    Output: %s
                    
                    """.formatted(total, proffOk, proffFailed, outputFile);
        }

        long aquaFound = rows.stream().filter(OrgNrReportSummary::isAquaFound).count();
        long aquaNotFound = rows.stream().filter(OrgNrReportSummary::isAquaNotFound).count();
        long aquaNoMatching = rows.stream().filter(OrgNrReportSummary::isAquaNoMatching).count();

        return """
                --- Oppsummering ---
                
                Modus: Proff og MTB
                Antall linjer: %d
                Proff OK: %d
                Proff feilet: %d
                Aqua funnet: %d
                Aqua ikke funnet: %d
                Aqua uten treff: %d
                Output: %s
                
                """.formatted(
                total,
                proffOk,
                proffFailed,
                aquaFound,
                aquaNotFound,
                aquaNoMatching,
                outputFile
        );
    }

    private static boolean isProffOk(OrgNrReportData row) {
        String status = row.proffStatus();
        return status != null && (status.equals("OK") || status.startsWith("PARTIAL"));
    }

    private static boolean isProffFailed(OrgNrReportData row) {
        String status = row.proffStatus();
        return status != null && status.startsWith("FAILED");
    }

    private static boolean isAquaFound(OrgNrReportData row) {
        String status = row.aquaStatus();
        return status != null && (status.equals("OK") || status.startsWith("PARTIAL"));
    }

    private static boolean isAquaNotFound(OrgNrReportData row) {
        String status = row.aquaStatus();
        return status != null && status.startsWith("FAILED");
    }

    private static boolean isAquaNoMatching(OrgNrReportData row) {
        return "OK: NO_MATCHING_MTB".equals(row.aquaStatus());
    }
}
