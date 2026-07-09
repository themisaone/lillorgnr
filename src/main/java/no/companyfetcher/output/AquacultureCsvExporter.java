package no.companyfetcher.output;

import no.companyfetcher.model.AquacultureCapacityData;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class AquacultureCsvExporter {

    private static final Logger log = LoggerFactory.getLogger(AquacultureCsvExporter.class);

    private final Path outputFile;

    public AquacultureCsvExporter(Path outputFile) {
        this.outputFile = outputFile;
    }

    public void export(List<AquacultureCapacityData> rows) {
        try {
            if (outputFile.getParent() != null) {
                Files.createDirectories(outputFile.getParent());
            }

            try (Writer writer = Files.newBufferedWriter(outputFile);
                 CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT.builder()
                         .setHeader("OrgNr", "OrgName", "TotalCapacity", "Unit", "EntryCount", "Status")
                         .build())) {

                for (AquacultureCapacityData row : rows) {
                    printer.printRecord(
                            row.orgNumber(),
                            nullToEmpty(row.companyName()),
                            formatDouble(row.totalCapacity()),
                            nullToEmpty(row.unit()),
                            row.entryCount(),
                            row.status()
                    );
                }
            }

            log.info("Exported {} rows to {}", rows.size(), outputFile.toAbsolutePath());
        } catch (IOException e) {
            throw new IllegalStateException("Failed to write CSV output: " + outputFile, e);
        }
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
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
}
