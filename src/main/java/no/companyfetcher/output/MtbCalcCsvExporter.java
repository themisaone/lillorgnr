package no.companyfetcher.output;

import no.companyfetcher.model.MtbCalcData;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class MtbCalcCsvExporter {

    private static final Logger log = LoggerFactory.getLogger(MtbCalcCsvExporter.class);

    private final Path outputFile;

    public MtbCalcCsvExporter(Path outputFile) {
        this.outputFile = outputFile;
    }

    public void export(List<MtbCalcData> rows) {
        try {
            if (outputFile.getParent() != null) {
                Files.createDirectories(outputFile.getParent());
            }

            try (Writer writer = Files.newBufferedWriter(outputFile);
                 CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT.builder()
                         .setHeader("Name", "MTB", "Fee", "MedlCont", "ServAvgift")
                         .build())) {

                for (MtbCalcData row : rows) {
                    printer.printRecord(
                            row.name(),
                            formatDouble(row.mtb()),
                            formatLong(row.fee()),
                            formatLong(row.medlCont()),
                            formatLong(row.servAvgift())
                    );
                }
            }

            log.info("Exported {} rows to {}", rows.size(), outputFile.toAbsolutePath());
        } catch (IOException e) {
            throw new IllegalStateException("Failed to write CSV output: " + outputFile, e);
        }
    }

    private static String formatDouble(Double value) {
        if (value == null) {
            return "";
        }
        if (value == value.longValue()) {
            return Long.toString(value.longValue());
        }
        return value.toString();
    }

    private static String formatLong(Long value) {
        return value == null ? "" : value.toString();
    }
}
