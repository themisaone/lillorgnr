package no.companyfetcher.service;

import no.companyfetcher.config.Configuration;
import no.companyfetcher.input.AquacultureCsvReader;
import no.companyfetcher.model.AquacultureCapacityData;
import no.companyfetcher.output.AquaExcelUpdater;
import no.companyfetcher.output.ExcelMergeOptions;

import java.nio.file.Path;
import java.util.Map;

public class AquaExcelMergeService {

    public AquaExcelUpdater.MergeResult merge(Configuration configuration, ExcelMergeOptions options) {
        Path csvFile = Path.of(configuration.getAquaOutputFile());
        Path excelFile = Path.of(configuration.getExcelFile());

        Map<String, AquacultureCapacityData> rows = new AquacultureCsvReader(csvFile).readByOrgNumber();
        AquaExcelUpdater updater = new AquaExcelUpdater(excelFile, options);
        return updater.update(rows);
    }
}
