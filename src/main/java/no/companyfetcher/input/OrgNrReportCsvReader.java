package no.companyfetcher.input;

import no.companyfetcher.model.AquacultureCapacityData;
import no.companyfetcher.model.CompanyData;
import no.companyfetcher.model.OrgNrReportData;
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

public class OrgNrReportCsvReader {

    private static final Logger log = LoggerFactory.getLogger(OrgNrReportCsvReader.class);

    private final Path csvFile;

    public OrgNrReportCsvReader(Path csvFile) {
        this.csvFile = csvFile;
    }

    public Map<String, OrgNrReportData> readByOrgNumber() {
        if (!Files.exists(csvFile)) {
            throw new IllegalStateException("CSV file not found: " + csvFile.toAbsolutePath());
        }

        Map<String, OrgNrReportData> rows = new LinkedHashMap<>();

        try (Reader reader = Files.newBufferedReader(csvFile);
             CSVParser parser = CSVFormat.DEFAULT.builder()
                     .setHeader()
                     .setSkipHeaderRecord(true)
                     .build()
                     .parse(reader)) {

            for (CSVRecord record : parser) {
                OrgNrReportData row = parseRecord(record);
                rows.put(row.orgNumber(), row);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read CSV file: " + csvFile, e);
        }

        log.info("Read {} report rows from {}", rows.size(), csvFile.getFileName());
        return rows;
    }

    public Map<String, CompanyData> readCompanyDataByOrgNumber() {
        Map<String, CompanyData> companies = new LinkedHashMap<>();
        for (OrgNrReportData row : readByOrgNumber().values()) {
            companies.put(row.orgNumber(), row.toCompanyData());
        }
        return companies;
    }

    public Map<String, AquacultureCapacityData> readAquacultureDataByOrgNumber() {
        Map<String, AquacultureCapacityData> rows = new LinkedHashMap<>();
        for (OrgNrReportData row : readByOrgNumber().values()) {
            rows.put(row.orgNumber(), row.toAquacultureData());
        }
        return rows;
    }

    private static OrgNrReportData parseRecord(CSVRecord record) {
        String orgNumber = normalizeOrgNumber(record.get("OrgNr"));
        return new OrgNrReportData(
                orgNumber,
                emptyToNull(record.get("OrgName")),
                parseInteger(record.get("AccountingYear")),
                parseLong(record.get("Revenue")),
                parseLong(record.get("SalaryCost")),
                parseLong(record.get("EBIT")),
                readStatus(record, "ProffStatus", "Status"),
                parseDouble(record.get("TotalCapacity")),
                emptyToNull(record.get("Unit")),
                parseEntryCount(record.get("EntryCount")),
                readStatus(record, "AquaStatus", "Status"),
                parseLong(record.get("Fee"))
        );
    }

    private static String readStatus(CSVRecord record, String primaryHeader, String fallbackHeader) {
        if (record.isMapped(primaryHeader)) {
            return record.get(primaryHeader);
        }
        if (record.isMapped(fallbackHeader)) {
            return record.get(fallbackHeader);
        }
        return "";
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

    private static int parseEntryCount(String value) {
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
