package no.companyfetcher.output;

import no.companyfetcher.model.CompanyData;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public class CsvExporter implements CompanyExporter {

    private static final Logger log = LoggerFactory.getLogger(CsvExporter.class);

    private final Path outputFile;

    public CsvExporter(Path outputFile) {
        this.outputFile = outputFile;
    }

    @Override
    public void export(java.util.List<CompanyData> companies) {
        try {
            if (outputFile.getParent() != null) {
                Files.createDirectories(outputFile.getParent());
            }

            try (Writer writer = Files.newBufferedWriter(outputFile);
                 CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT.builder()
                         .setHeader("OrgNr", "OrgName", "AccountingYear", "Revenue", "SalaryCost", "EBIT", "Status")
                         .build())) {

                for (CompanyData company : companies) {
                    printer.printRecord(
                            company.orgNumber(),
                            nullToEmpty(company.companyName()),
                            formatInteger(company.accountingYear()),
                            formatLong(company.revenue()),
                            formatLong(company.salaryCost()),
                            formatLong(company.ebit()),
                            company.status()
                    );
                }
            }

            log.info("Exported {} rows to {}", companies.size(), outputFile.toAbsolutePath());
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
}
