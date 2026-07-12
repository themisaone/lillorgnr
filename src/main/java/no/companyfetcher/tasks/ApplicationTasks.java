package no.companyfetcher.tasks;

import no.companyfetcher.config.Configuration;
import no.companyfetcher.input.CompanyInputReader;
import no.companyfetcher.input.TextFileReader;
import no.companyfetcher.model.OrgNrReportData;
import no.companyfetcher.model.ReportRunMode;
import no.companyfetcher.output.ExcelMergeOptions;
import no.companyfetcher.output.HighlightColor;
import no.companyfetcher.output.OrgNrReportCsvExporter;
import no.companyfetcher.output.ReportExcelUpdater;
import no.companyfetcher.service.AquaExcelMergeService;
import no.companyfetcher.service.ExcelMergeService;
import no.companyfetcher.service.OrgNrReportService;
import no.companyfetcher.service.OrgNrReportSummary;
import no.companyfetcher.service.ReportExcelMergeService;

import java.nio.file.Path;
import java.util.List;

public final class ApplicationTasks {

    private ApplicationTasks() {
    }

    public static String fetchAll(Configuration configuration) {
        return fetchAll(configuration, ReportRunMode.PROFF_AND_MTB);
    }

    public static String fetchAll(Configuration configuration, ReportRunMode mode) {
        CompanyInputReader inputReader = new TextFileReader(Path.of(configuration.getProffAquaInputFile()));
        OrgNrReportService reportService = new OrgNrReportService(configuration);
        OrgNrReportCsvExporter exporter = new OrgNrReportCsvExporter(Path.of(configuration.getOutputFile()));

        List<String> orgNumbers = inputReader.read();
        List<OrgNrReportData> rows = reportService.fetchAll(orgNumbers, mode);
        exporter.export(rows);

        return OrgNrReportSummary.format(rows, configuration.getOutputFile(), mode);
    }

    public static String mergeToExcel(Configuration configuration, ReportRunMode mode, String highlightColorName) {
        return mergeToExcel(configuration, mode, ExcelMergeOptions.withHighlight(HighlightColor.parse(highlightColorName)));
    }

    public static String mergeToExcel(Configuration configuration, ReportRunMode mode, ExcelMergeOptions mergeOptions) {
        ReportExcelUpdater.Scope scope = mode == ReportRunMode.PROFF_ONLY
                ? ReportExcelUpdater.Scope.PROF
                : ReportExcelUpdater.Scope.ALL;

        ReportExcelUpdater.MergeResult result = new ReportExcelMergeService()
                .merge(configuration, scope, mergeOptions);

        String columns = mode == ReportRunMode.PROFF_ONLY ? "E, G, I" : "E, G, I, K, M";
        String modeLabel = mode == ReportRunMode.PROFF_ONLY ? "Bare Proff" : "Proff og MTB";

        return """
                Excel-sammenslåing fullført.
                Modus: %s
                Oppdaterte rader: %d
                Rader uten CSV-treff: %d
                Kolonner: %s
                Markeringsfarge: %s
                CSV-kilde: %s
                Excel-fil: %s
                
                Husk: lukk Excel før sammenslåing.
                """.formatted(
                modeLabel,
                result.updatedRows(),
                result.skippedRows(),
                columns,
                describeHighlight(mergeOptions),
                configuration.getOutputFile(),
                configuration.getExcelFile()
        );
    }

    public static String mergeAllToExcel(Configuration configuration, String highlightColorName) {
        return mergeToExcel(configuration, ReportRunMode.PROFF_AND_MTB, highlightColorName);
    }

    public static String mergeAllToExcel(Configuration configuration, ExcelMergeOptions mergeOptions) {
        return mergeToExcel(configuration, ReportRunMode.PROFF_AND_MTB, mergeOptions);
    }

    public static String mergeProffToExcel(Configuration configuration, String highlightColorName) {
        return mergeProffToExcel(configuration, ExcelMergeOptions.withHighlight(HighlightColor.parse(highlightColorName)));
    }

    public static String mergeProffToExcel(Configuration configuration, ExcelMergeOptions mergeOptions) {
        ReportExcelUpdater.MergeResult result = new ExcelMergeService().merge(configuration, mergeOptions);

        return """
                Proff merge to Excel completed.
                Updated rows: %d
                Rows without CSV match: %d
                Columns: E, G, I
                Highlight: %s
                CSV source: %s
                Excel file: %s
                
                Remember: close Excel before merging.
                """.formatted(
                result.updatedRows(),
                result.skippedRows(),
                describeHighlight(mergeOptions),
                configuration.getOutputFile(),
                configuration.getExcelFile()
        );
    }

    public static String mergeAquaToExcel(Configuration configuration, String highlightColorName) {
        return mergeAquaToExcel(configuration, ExcelMergeOptions.withHighlight(HighlightColor.parse(highlightColorName)));
    }

    public static String mergeAquaToExcel(Configuration configuration, ExcelMergeOptions mergeOptions) {
        ReportExcelUpdater.MergeResult result = new AquaExcelMergeService().merge(configuration, mergeOptions);

        return """
                Aqua merge to Excel completed.
                Updated rows: %d
                Rows without CSV match: %d
                Column: K
                Highlight: %s
                CSV source: %s
                Excel file: %s
                
                Remember: close Excel before merging.
                """.formatted(
                result.updatedRows(),
                result.skippedRows(),
                describeHighlight(mergeOptions),
                configuration.getOutputFile(),
                configuration.getExcelFile()
        );
    }

    public static String mergeFeeToExcel(Configuration configuration, String highlightColorName) {
        return mergeFeeToExcel(configuration, ExcelMergeOptions.withHighlight(HighlightColor.parse(highlightColorName)));
    }

    public static String mergeFeeToExcel(Configuration configuration, ExcelMergeOptions mergeOptions) {
        ReportExcelUpdater.MergeResult result = new ReportExcelMergeService()
                .merge(configuration, ReportExcelUpdater.Scope.FEE, mergeOptions);

        return """
                MTB fee merge to Excel completed.
                Updated rows: %d
                Rows without CSV match: %d
                Column: M
                Highlight: %s
                CSV source: %s
                Excel file: %s
                
                Remember: close Excel before merging.
                """.formatted(
                result.updatedRows(),
                result.skippedRows(),
                describeHighlight(mergeOptions),
                configuration.getOutputFile(),
                configuration.getExcelFile()
        );
    }

    private static String describeHighlight(ExcelMergeOptions mergeOptions) {
        if (!mergeOptions.highlightUpdatedCells()) {
            return "disabled";
        }
        return String.valueOf(mergeOptions.highlightColor());
    }
}
