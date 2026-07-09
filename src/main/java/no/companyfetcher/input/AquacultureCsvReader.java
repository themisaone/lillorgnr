package no.companyfetcher.input;

import no.companyfetcher.model.AquacultureCapacityData;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public class AquacultureCsvReader {

    private static final Logger log = LoggerFactory.getLogger(AquacultureCsvReader.class);

    private final Path csvFile;

    public AquacultureCsvReader(Path csvFile) {
        this.csvFile = csvFile;
    }

    public Map<String, AquacultureCapacityData> readByOrgNumber() {
        if (!Files.exists(csvFile)) {
            throw new IllegalStateException("CSV file not found: " + csvFile.toAbsolutePath());
        }

        Map<String, AquacultureCapacityData> rows = new LinkedHashMap<>();

        try (Reader reader = Files.newBufferedReader(csvFile);
             CSVParser parser = CSVFormat.DEFAULT.builder()
                     .setHeader()
                     .setSkipHeaderRecord(true)
                     .build()
                     .parse(reader)) {

            for (CSVRecord record : parser) {
                String orgNumber = normalizeOrgNumber(record.get("OrgNr"));
                AquacultureCapacityData row = new AquacultureCapacityData(
                        orgNumber,
                        emptyToNull(record.get("OrgName")),
                        parseDouble(record.get("TotalCapacity")),
                        emptyToNull(record.get("Unit")),
                        parseInteger(record.get("EntryCount")),
                        record.get("Status")
                );
                rows.put(orgNumber, row);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read CSV file: " + csvFile, e);
        }

        log.info("Read {} aquaculture rows from {}", rows.size(), csvFile.getFileName());
        return rows;
    }

    private static String normalizeOrgNumber(String orgNumber) {
        return orgNumber.replaceAll("\\D", "");
    }

    private static String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static Integer parseInteger(String value) {
        if (value == null || value.isBlank()) {
            return 0;
        }
        return Integer.parseInt(value.trim());
    }

    private static Double parseDouble(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return Double.parseDouble(value.trim());
    }
}
