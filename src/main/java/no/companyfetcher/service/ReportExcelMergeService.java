package no.companyfetcher.service;

import no.companyfetcher.config.Configuration;
import no.companyfetcher.input.OrgNrReportCsvReader;
import no.companyfetcher.output.ExcelMergeOptions;
import no.companyfetcher.output.ReportExcelUpdater;

import java.nio.file.Path;

public class ReportExcelMergeService {

    public ReportExcelUpdater.MergeResult merge(
            Configuration configuration,
            ReportExcelUpdater.Scope scope,
            ExcelMergeOptions options
    ) {
        Path csvFile = Path.of(configuration.getOutputFile());
        Path excelFile = Path.of(configuration.getExcelFile());

        var rows = new OrgNrReportCsvReader(csvFile).readByOrgNumber();
        return new ReportExcelUpdater(excelFile, options).update(rows, scope);
    }
}
