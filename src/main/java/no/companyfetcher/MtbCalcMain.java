package no.companyfetcher;

import no.companyfetcher.config.Configuration;
import no.companyfetcher.input.MtbInputReader;
import no.companyfetcher.model.MtbCalcData;
import no.companyfetcher.output.MtbCalcCsvExporter;
import no.companyfetcher.service.MtbCalcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class MtbCalcMain {

    private static final Logger log = LoggerFactory.getLogger(MtbCalcMain.class);

    public static void main(String[] args) {
        log.info("START {}", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));

        Configuration configuration = Configuration.load();
        MtbInputReader inputReader = new MtbInputReader(Path.of(configuration.getMtbInputFile()));
        MtbCalcService calcService = new MtbCalcService(configuration);
        MtbCalcCsvExporter exporter = new MtbCalcCsvExporter(Path.of(configuration.getMtbOutputFile()));

        List<MtbInputReader.MtbInputRow> inputRows = inputReader.read();
        System.out.println("Reading " + inputRows.size() + " konsern entries");

        List<MtbCalcData> rows = calcService.calculateAll(inputRows);
        exporter.export(rows);

        printProgress(inputRows.size());
        System.out.println();
        System.out.println("Completed:");
        System.out.println(rows.size() + " rows");
        System.out.println();
        System.out.println("Output:");
        System.out.println(configuration.getMtbOutputFile());

        log.info("FINISHED MTB calculation");
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
