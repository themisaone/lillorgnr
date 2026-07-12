package no.companyfetcher.output;

import no.companyfetcher.model.OrgNrReportData;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ReportExcelUpdaterTest {

    @TempDir
    Path tempDir;

    @Test
    void updatesAllColumnsIncludingFeeInColumnM() throws Exception {
        Path workbookPath = tempDir.resolve("companies.xlsx");
        createSampleWorkbook(workbookPath);

        Map<String, OrgNrReportData> rows = Map.of(
                "994613405", new OrgNrReportData(
                        "994613405", "Arnøy Laks AS", 2024,
                        248_204_000L, 25_194_000L, 15_303_000L, "OK",
                        4250.0, "MTB", 4, "OK", 73_830L
                )
        );

        ReportExcelUpdater updater = new ReportExcelUpdater(
                workbookPath,
                ExcelMergeOptions.withDefaultHighlight()
        );
        ReportExcelUpdater.MergeResult result = updater.update(rows, ReportExcelUpdater.Scope.ALL);

        assertEquals(1, result.updatedRows());

        try (var workbook = new XSSFWorkbook(workbookPath.toFile())) {
            Row row6 = workbook.getSheetAt(0).getRow(5);
            assertEquals(248_204_000.0, row6.getCell(4).getNumericCellValue());
            assertEquals(25_194_000.0, row6.getCell(6).getNumericCellValue());
            assertEquals(15_303_000.0, row6.getCell(8).getNumericCellValue());
            assertEquals(4250.0, row6.getCell(10).getNumericCellValue());
            assertEquals(73_830.0, row6.getCell(12).getNumericCellValue());
        }
    }

    @Test
    void updatesOnlyFeeInColumnM() throws Exception {
        Path workbookPath = tempDir.resolve("companies.xlsx");
        createSampleWorkbook(workbookPath);

        Map<String, OrgNrReportData> rows = Map.of(
                "994613405", new OrgNrReportData(
                        "994613405", "Arnøy Laks AS", 2024,
                        null, null, null, "OK",
                        null, null, 0, "OK: NO_MATCHING_MTB", 0L
                )
        );

        new ReportExcelUpdater(workbookPath, ExcelMergeOptions.withoutHighlight())
                .update(rows, ReportExcelUpdater.Scope.FEE);

        try (var workbook = new XSSFWorkbook(workbookPath.toFile())) {
            Row row6 = workbook.getSheetAt(0).getRow(5);
            assertEquals(0.0, row6.getCell(12).getNumericCellValue());
        }
    }

    private void createSampleWorkbook(Path workbookPath) throws Exception {
        try (var workbook = new XSSFWorkbook()) {
            var sheet = workbook.createSheet("Sheet1");
            var row6 = sheet.createRow(5);
            row6.createCell(1).setCellValue("994613405");
            try (var out = Files.newOutputStream(workbookPath)) {
                workbook.write(out);
            }
        }
    }
}
