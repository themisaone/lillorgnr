package no.companyfetcher.output;

import no.companyfetcher.model.OrgNrReportData;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class OrgNrReportCsvExporter {

    private static final Logger log = LoggerFactory.getLogger(OrgNrReportCsvExporter.class);

    private final Path outputFile;

    public OrgNrReportCsvExporter(Path outputFile) {
        this.outputFile = outputFile;
    }

    public void export(List<OrgNrReportData> rows) {
        try {
            if (outputFile.getParent() != null) {
                Files.createDirectories(outputFile.getParent());
            }

            try (Writer writer = Files.newBufferedWriter(outputFile);
                 CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT.builder()
                         .setHeader(
                                 "OrgNr",
                                 "OrgName",
                                 "AccountingYear",
                                 "Revenue",
                                 "SalaryCost",
                                 "EBIT",
                                 "ProffStatus",
                                 "TotalCapacity",
                                 "Unit",
                                 "EntryCount",
                                 "AquaStatus",
                                 "Fee"
                         )
                         .build())) {

                for (OrgNrReportData row : rows) {
                    printer.printRecord(
                            row.orgNumber(),
                            nullToEmpty(row.orgName()),
                            formatInteger(row.accountingYear()),
                            formatLong(row.revenue()),
                            formatLong(row.salaryCost()),
                            formatLong(row.ebit()),
                            row.proffStatus(),
                            formatDouble(row.totalCapacity()),
                            nullToEmpty(row.unit()),
                            row.entryCount(),
                            row.aquaStatus(),
                            formatLong(row.fee())
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

    private static String formatInteger(Integer value) {
        return value == null ? "" : value.toString();
    }

    private static String formatLong(Long value) {
        return value == null ? "" : value.toString();
    }

    private static String formatDouble(Double value) {
        if (value == null) {
            return "";
        }
        if (value == Math.rint(value)) {
            return String.valueOf(value.longValue());
        }
        return value.toString();
    }
}
