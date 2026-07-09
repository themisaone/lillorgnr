package no.companyfetcher.service;

import no.companyfetcher.config.Configuration;
import no.companyfetcher.input.CsvCompanyReader;
import no.companyfetcher.model.CompanyData;
import no.companyfetcher.output.ExcelMergeOptions;
import no.companyfetcher.output.ExcelUpdater;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.Map;

public class ExcelMergeService {

    private static final Logger log = LoggerFactory.getLogger(ExcelMergeService.class);

    public ExcelUpdater.MergeResult merge(Configuration configuration, ExcelMergeOptions options) {
        Path csvFile = Path.of(configuration.getOutputFile());
        Path excelFile = Path.of(configuration.getExcelFile());

        Map<String, CompanyData> companies = new CsvCompanyReader(csvFile).readByOrgNumber();
        ExcelUpdater updater = new ExcelUpdater(excelFile, options);
        return updater.update(companies);
    }
}
