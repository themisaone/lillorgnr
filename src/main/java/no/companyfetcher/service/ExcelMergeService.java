package no.companyfetcher.service;

import no.companyfetcher.config.Configuration;
import no.companyfetcher.output.ExcelMergeOptions;
import no.companyfetcher.output.ReportExcelUpdater;

public class ExcelMergeService {

    public ReportExcelUpdater.MergeResult merge(Configuration configuration, ExcelMergeOptions options) {
        return new ReportExcelMergeService().merge(configuration, ReportExcelUpdater.Scope.PROF, options);
    }
}
