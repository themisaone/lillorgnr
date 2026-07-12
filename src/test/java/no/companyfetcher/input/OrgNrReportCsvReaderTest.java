package no.companyfetcher.input;

import no.companyfetcher.model.OrgNrReportData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OrgNrReportCsvReaderTest {

    @TempDir
    Path tempDir;

    @Test
    void readsUnifiedReportCsv() throws Exception {
        Path csv = tempDir.resolve("OrgNrReport.csv");
        Files.writeString(csv, """
                OrgNr,OrgName,AccountingYear,Revenue,SalaryCost,EBIT,ProffStatus,TotalCapacity,Unit,EntryCount,AquaStatus,Fee
                994613405,Arnøy Laks AS,2024,248204000,25194000,15303000,OK,4250,MTB,3,OK,73830
                """);

        OrgNrReportData row = new OrgNrReportCsvReader(csv).readByOrgNumber().get("994613405");

        assertEquals("Arnøy Laks AS", row.orgName());
        assertEquals(2024, row.accountingYear());
        assertEquals(248204000L, row.revenue());
        assertEquals("OK", row.proffStatus());
        assertEquals(4250.0, row.totalCapacity());
        assertEquals("OK", row.aquaStatus());
        assertEquals(73830L, row.fee());
        assertEquals(248204000L, row.toCompanyData().revenue());
        assertEquals(4250.0, row.toAquacultureData().totalCapacity());
    }
}
