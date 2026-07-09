package no.companyfetcher.input;

import no.companyfetcher.model.CompanyData;
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

public class CsvCompanyReader {

    private static final Logger log = LoggerFactory.getLogger(CsvCompanyReader.class);

    private final Path csvFile;

    public CsvCompanyReader(Path csvFile) {
        this.csvFile = csvFile;
    }

    public Map<String, CompanyData> readByOrgNumber() {
        if (!Files.exists(csvFile)) {
            throw new IllegalStateException("CSV file not found: " + csvFile.toAbsolutePath());
        }

        Map<String, CompanyData> companies = new LinkedHashMap<>();

        try (Reader reader = Files.newBufferedReader(csvFile);
             CSVParser parser = CSVFormat.DEFAULT.builder()
                     .setHeader()
                     .setSkipHeaderRecord(true)
                     .build()
                     .parse(reader)) {

            for (CSVRecord record : parser) {
                String orgNumber = normalizeOrgNumber(record.get("OrgNr"));
                CompanyData company = new CompanyData(
                        orgNumber,
                        emptyToNull(record.get("OrgName")),
                        parseInteger(record.get("AccountingYear")),
                        parseLong(record.get("Revenue")),
                        parseLong(record.get("SalaryCost")),
                        parseLong(record.get("EBIT")),
                        record.get("Status")
                );
                companies.put(orgNumber, company);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read CSV file: " + csvFile, e);
        }

        log.info("Read {} companies from {}", companies.size(), csvFile.getFileName());
        return companies;
    }

    private static String normalizeOrgNumber(String orgNumber) {
        return orgNumber.replaceAll("\\D", "");
    }

    private static String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static Integer parseInteger(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return Integer.parseInt(value.trim());
    }

    private static Long parseLong(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return Long.parseLong(value.trim());
    }
}
