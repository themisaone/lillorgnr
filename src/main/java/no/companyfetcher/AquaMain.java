package no.companyfetcher;

import no.companyfetcher.cli.CommandLineArgs;
import no.companyfetcher.config.Configuration;
import no.companyfetcher.input.CompanyInputReader;
import no.companyfetcher.input.TextFileReader;
import no.companyfetcher.model.AquacultureCapacityData;
import no.companyfetcher.output.AquacultureCsvExporter;
import no.companyfetcher.output.AquaExcelUpdater;
import no.companyfetcher.output.ExcelMergeOptions;
import no.companyfetcher.provider.FiskeridirApiProvider;
import no.companyfetcher.service.AquaExcelMergeService;
import no.companyfetcher.service.AquacultureService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class AquaMain {

    private static final Logger log = LoggerFactory.getLogger(AquaMain.class);

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
        CompanyInputReader inputReader = new TextFileReader(Path.of(configuration.getInputFile()));
        AquacultureService aquacultureService = new AquacultureService(new FiskeridirApiProvider(configuration), configuration);
        AquacultureCsvExporter exporter = new AquacultureCsvExporter(Path.of(configuration.getAquaOutputFile()));

        List<String> orgNumbers = inputReader.read();
        System.out.println("Reading " + orgNumbers.size() + " companies");

        List<AquacultureCapacityData> rows = aquacultureService.fetchAll(orgNumbers);
        exporter.export(rows);

        long okCount = rows.stream().filter(r -> r.status() != null && r.status().startsWith("OK")).count();
        long failedCount = rows.size() - okCount;

        printProgress(orgNumbers.size());
        System.out.println();
        System.out.println("Completed:");
        System.out.println(okCount + " OK");
        System.out.println(failedCount + " FAILED");
        System.out.println();
        System.out.println("Output:");
        System.out.println(configuration.getAquaOutputFile());

        log.info("FINISHED aquaculture fetch");
    }

    private static void runExcelMerge(Configuration configuration, String[] args) {
        ExcelMergeOptions mergeOptions = CommandLineArgs.parseMergeOptions(args, "AquaFetcher.jar");
        AquaExcelMergeService mergeService = new AquaExcelMergeService();
        AquaExcelUpdater.MergeResult result = mergeService.merge(configuration, mergeOptions);

        System.out.println("Merging aquaculture CSV into Excel");
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
        System.out.println(configuration.getAquaOutputFile());
        System.out.println("Excel target:");
        System.out.println(configuration.getExcelFile());

        log.info("FINISHED aquaculture excel merge");
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
