package no.companyfetcher.tasks;

import no.companyfetcher.config.Configuration;
import no.companyfetcher.input.CompanyInputReader;
import no.companyfetcher.input.MtbInputReader;
import no.companyfetcher.input.TextFileReader;
import no.companyfetcher.model.AquacultureCapacityData;
import no.companyfetcher.model.CompanyData;
import no.companyfetcher.model.MtbCalcData;
import no.companyfetcher.output.AquacultureCsvExporter;
import no.companyfetcher.output.AquaExcelUpdater;
import no.companyfetcher.output.CompanyExporter;
import no.companyfetcher.output.CsvExporter;
import no.companyfetcher.output.ExcelMergeOptions;
import no.companyfetcher.output.ExcelUpdater;
import no.companyfetcher.output.HighlightColor;
import no.companyfetcher.output.MtbCalcCsvExporter;
import no.companyfetcher.provider.CompanyProvider;
import no.companyfetcher.provider.DummyProvider;
import no.companyfetcher.provider.FiskeridirApiProvider;
import no.companyfetcher.provider.ProffApiProvider;
import no.companyfetcher.provider.ProffWebProvider;
import no.companyfetcher.provider.ProviderType;
import no.companyfetcher.service.AquaExcelMergeService;
import no.companyfetcher.service.AquacultureService;
import no.companyfetcher.service.CompanyService;
import no.companyfetcher.service.ExcelMergeService;
import no.companyfetcher.service.MtbCalcService;

import java.nio.file.Path;
import java.util.List;

public final class ApplicationTasks {

    private ApplicationTasks() {
    }

    public static String fetchProff(Configuration configuration) {
        CompanyInputReader inputReader = new TextFileReader(Path.of(configuration.getProffAquaInputFile()));
        CompanyProvider provider = createProffProvider(configuration);
        CompanyService companyService = new CompanyService(provider, configuration);
        CompanyExporter exporter = new CsvExporter(Path.of(configuration.getProffOutputFile()));

        List<String> orgNumbers = inputReader.read();
        List<CompanyData> companies = companyService.fetchAll(orgNumbers);
        exporter.export(companies);

        long okCount = companies.stream()
                .filter(c -> "OK".equals(c.status()) || "PARTIAL".equals(c.status()))
                .count();

        return """
                Fetch from Proff completed.
                Companies read: %d
                OK: %d
                Failed: %d
                Output: %s
                """.formatted(orgNumbers.size(), okCount, companies.size() - okCount, configuration.getProffOutputFile());
    }

    public static String mergeProffToExcel(Configuration configuration, String highlightColorName) {
        ExcelMergeOptions mergeOptions = ExcelMergeOptions.withHighlight(HighlightColor.parse(highlightColorName));
        ExcelUpdater.MergeResult result = new ExcelMergeService().merge(configuration, mergeOptions);

        return """
                Proff merge to Excel completed.
                Updated rows: %d
                Rows without CSV match: %d
                Highlight: %s
                CSV source: %s
                Excel file: %s
                
                Remember: close Excel before merging.
                """.formatted(
                result.updatedRows(),
                result.skippedRows(),
                highlightColorName,
                configuration.getProffOutputFile(),
                configuration.getExcelFile()
        );
    }

    public static String fetchAqua(Configuration configuration) {
        CompanyInputReader inputReader = new TextFileReader(Path.of(configuration.getProffAquaInputFile()));
        AquacultureService aquacultureService = new AquacultureService(new FiskeridirApiProvider(configuration), configuration);
        AquacultureCsvExporter exporter = new AquacultureCsvExporter(Path.of(configuration.getAquaOutputFile()));

        List<String> orgNumbers = inputReader.read();
        List<AquacultureCapacityData> rows = aquacultureService.fetchAll(orgNumbers);
        exporter.export(rows);

        long okCount = rows.stream().filter(r -> r.status() != null && r.status().startsWith("OK")).count();

        return """
                AquaFetcher fetch completed.
                Companies read: %d
                OK: %d
                Failed: %d
                Output: %s
                """.formatted(orgNumbers.size(), okCount, rows.size() - okCount, configuration.getAquaOutputFile());
    }

    public static String mergeAquaToExcel(Configuration configuration, String highlightColorName) {
        ExcelMergeOptions mergeOptions = ExcelMergeOptions.withHighlight(HighlightColor.parse(highlightColorName));
        AquaExcelUpdater.MergeResult result = new AquaExcelMergeService().merge(configuration, mergeOptions);

        return """
                Aqua merge to Excel completed.
                Updated rows: %d
                Rows without CSV match: %d
                Highlight: %s
                CSV source: %s
                Excel file: %s
                
                Remember: close Excel before merging.
                """.formatted(
                result.updatedRows(),
                result.skippedRows(),
                highlightColorName,
                configuration.getAquaOutputFile(),
                configuration.getExcelFile()
        );
    }

    public static String runMtbCalc(Configuration configuration) {
        MtbInputReader inputReader = new MtbInputReader(Path.of(configuration.getMtbInputFile()));
        MtbCalcService calcService = new MtbCalcService(configuration);
        MtbCalcCsvExporter exporter = new MtbCalcCsvExporter(Path.of(configuration.getMtbOutputFile()));

        List<MtbInputReader.MtbInputRow> inputRows = inputReader.read();
        List<MtbCalcData> rows = calcService.calculateAll(inputRows);
        exporter.export(rows);

        return """
                MTB calculation completed.
                Rows: %d
                Input: %s
                Output: %s
                """.formatted(rows.size(), configuration.getMtbInputFile(), configuration.getMtbOutputFile());
    }

    private static CompanyProvider createProffProvider(Configuration configuration) {
        ProviderType providerType = configuration.getProviderType();
        return switch (providerType) {
            case PROFF_WEB -> new ProffWebProvider(configuration);
            case PROFF_API -> new ProffApiProvider(configuration);
            case DUMMY -> new DummyProvider(configuration.getAccountingYear());
        };
    }
}
