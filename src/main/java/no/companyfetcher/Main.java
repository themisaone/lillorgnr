package no.companyfetcher;

import no.companyfetcher.cli.CommandLineArgs;
import no.companyfetcher.config.Configuration;
import no.companyfetcher.input.CompanyInputReader;
import no.companyfetcher.input.TextFileReader;
import no.companyfetcher.model.CompanyData;
import no.companyfetcher.output.CompanyExporter;
import no.companyfetcher.output.CsvExporter;
import no.companyfetcher.output.ExcelMergeOptions;
import no.companyfetcher.output.ExcelUpdater;
import no.companyfetcher.provider.CompanyProvider;
import no.companyfetcher.provider.DummyProvider;
import no.companyfetcher.provider.ProffApiProvider;
import no.companyfetcher.provider.ProffWebProvider;
import no.companyfetcher.provider.ProviderType;
import no.companyfetcher.service.CompanyService;
import no.companyfetcher.service.ExcelMergeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class Main {

    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        log.info("START {}", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));

        Configuration configuration = Configuration.load();

        if (CommandLineArgs.isMergeExcelMode(args)) {
            runExcelMerge(configuration, args);
            return;
        }

        runFetch(configuration);
    }

    private static void runFetch(Configuration configuration) {
        CompanyInputReader inputReader = new TextFileReader(Path.of(configuration.getProffAquaInputFile()));
        CompanyProvider provider = createProvider(configuration);
        CompanyService companyService = new CompanyService(provider, configuration);
        CompanyExporter exporter = new CsvExporter(Path.of(configuration.getProffOutputFile()));

        List<String> orgNumbers = inputReader.read();
        System.out.println("Reading " + orgNumbers.size() + " companies");

        List<CompanyData> companies = companyService.fetchAll(orgNumbers);
        exporter.export(companies);

        long okCount = companies.stream().filter(c -> "OK".equals(c.status()) || "PARTIAL".equals(c.status())).count();
        long failedCount = companies.size() - okCount;

        printProgress(orgNumbers.size());
        System.out.println();
        System.out.println("Completed:");
        System.out.println(okCount + " OK");
        System.out.println(failedCount + " FAILED");
        System.out.println();
        System.out.println("Output:");
        System.out.println(configuration.getProffOutputFile());

        log.info("FINISHED");
    }

    private static void runExcelMerge(Configuration configuration, String[] args) {
        ExcelMergeOptions mergeOptions = CommandLineArgs.parseMergeOptions(args, "CompanyFetcher.jar");
        ExcelMergeService mergeService = new ExcelMergeService();
        ExcelUpdater.MergeResult result = mergeService.merge(configuration, mergeOptions);

        System.out.println("Merging CSV into Excel");
        System.out.println();
        System.out.println("Updated rows: " + result.updatedRows());
        System.out.println("Rows without CSV match: " + result.skippedRows());
        if (result.highlighted()) {
            System.out.println("Highlight: enabled (" + mergeOptions.highlightColor() + ")");
        } else {
            System.out.println("Highlight: disabled");
        }
        System.out.println();
        System.out.println("CSV source:");
        System.out.println(configuration.getProffOutputFile());
        System.out.println("Excel target:");
        System.out.println(configuration.getExcelFile());

        log.info("FINISHED excel merge");
    }

    private static CompanyProvider createProvider(Configuration configuration) {
        ProviderType providerType = configuration.getProviderType();
        return switch (providerType) {
            case PROFF_WEB -> new ProffWebProvider(configuration);
            case PROFF_API -> new ProffApiProvider(configuration);
            case DUMMY -> new DummyProvider(configuration.getAccountingYear());
        };
    }

    private static void printProgress(int total) {
        int barLength = 10;
        StringBuilder bar = new StringBuilder();
        for (int i = 0; i < barLength; i++) {
            bar.append('█');
        }
        System.out.println();
        System.out.println(bar + " 100%");
    }
}
