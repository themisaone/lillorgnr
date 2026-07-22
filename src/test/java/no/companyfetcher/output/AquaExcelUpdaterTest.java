package no.companyfetcher.output;

import no.companyfetcher.model.AquacultureCapacityData;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AquaExcelUpdaterTest {

    @TempDir
    Path tempDir;

    @Test
    void updatesMatchingOrgNumbersInColumnK() throws Exception {
        Path workbookPath = tempDir.resolve("companies.xlsx");
        createSampleWorkbook(workbookPath);

        Map<String, AquacultureCapacityData> rows = Map.of(
                "994613405", new AquacultureCapacityData("994613405", "ARNØY LAKS AS", 4250.0, "TN", 4, "OK"),
                "895366722", new AquacultureCapacityData("895366722", "ARNØY LAKS SLAKTERI AS", 1000.0, "TN", 2, "OK")
        );

        AquaExcelUpdater updater = new AquaExcelUpdater(workbookPath, ExcelMergeOptions.withDefaultHighlight());
        AquaExcelUpdater.MergeResult result = updater.update(rows);

        assertEquals(2, result.updatedRows());
        assertEquals(true, result.highlighted());

        try (var workbook = new XSSFWorkbook(workbookPath.toFile())) {
            Row row6 = workbook.getSheetAt(0).getRow(5);
            assertEquals(4250.0, row6.getCell(10).getNumericCellValue());
            assertEquals(FillPatternType.SOLID_FOREGROUND, row6.getCell(10).getCellStyle().getFillPattern());

            Row row7 = workbook.getSheetAt(0).getRow(6);
            assertEquals(1000.0, row7.getCell(10).getNumericCellValue());
        }
    }

    @Test
    void leavesCellUntouchedWhenCapacityIsEmpty() throws Exception {
        Path workbookPath = tempDir.resolve("companies.xlsx");
        short originalStyleIndex;
        try (var workbook = new XSSFWorkbook()) {
            var sheet = workbook.createSheet("Sheet1");
            var row = sheet.createRow(5);
            row.createCell(1).setCellValue("994613405");
            var cell = row.createCell(10);
            cell.setCellValue(999);
            var style = workbook.createCellStyle();
            style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            cell.setCellStyle(style);
            originalStyleIndex = style.getIndex();
            try (var out = Files.newOutputStream(workbookPath)) {
                workbook.write(out);
            }
        }

        Map<String, AquacultureCapacityData> rows = Map.of(
                "994613405", new AquacultureCapacityData("994613405", "ELVEVOLL SETTEFISK AS", null, null, 0, "OK: NO_MATCHING_MTB")
        );

        AquaExcelUpdater.MergeResult result = new AquaExcelUpdater(workbookPath, ExcelMergeOptions.withDefaultHighlight())
                .update(rows);

        assertEquals(0, result.updatedRows());

        try (var workbook = new XSSFWorkbook(workbookPath.toFile())) {
            var cell = workbook.getSheetAt(0).getRow(5).getCell(10);
            assertEquals(999.0, cell.getNumericCellValue());
            assertEquals(originalStyleIndex, cell.getCellStyle().getIndex());
        }
    }

    @Test
    void clearsCellAndHighlightsWhenCapacityIsEmptyAndConfiguredToClear() throws Exception {
        Path workbookPath = tempDir.resolve("companies.xlsx");
        try (var workbook = new XSSFWorkbook()) {
            var sheet = workbook.createSheet("Sheet1");
            var row = sheet.createRow(5);
            row.createCell(1).setCellValue("994613405");
            row.createCell(10).setCellValue(999);
            try (var out = Files.newOutputStream(workbookPath)) {
                workbook.write(out);
            }
        }

        Map<String, AquacultureCapacityData> rows = Map.of(
                "994613405", new AquacultureCapacityData("994613405", "ELVEVOLL SETTEFISK AS", null, null, 0, "OK: NO_MATCHING_MTB")
        );

        AquaExcelUpdater.MergeResult result = new AquaExcelUpdater(
                workbookPath,
                ExcelMergeOptions.withDefaultHighlight().withEmptyValueProcessing(EmptyValueProcessing.CLEAR)
        ).update(rows);

        assertEquals(1, result.updatedRows());

        try (var workbook = new XSSFWorkbook(workbookPath.toFile())) {
            var cell = workbook.getSheetAt(0).getRow(5).getCell(10);
            assertEquals(org.apache.poi.ss.usermodel.CellType.BLANK, cell.getCellType());
            assertEquals(FillPatternType.SOLID_FOREGROUND, cell.getCellStyle().getFillPattern());
        }
    }

    private void createSampleWorkbook(Path workbookPath) throws Exception {
        try (var workbook = new XSSFWorkbook()) {
            var sheet = workbook.createSheet("Sheet1");
            var row6 = sheet.createRow(5);
            row6.createCell(1).setCellValue("994613405");
            var row7 = sheet.createRow(6);
            row7.createCell(1).setCellValue("895366722");
            try (var out = Files.newOutputStream(workbookPath)) {
                workbook.write(out);
            }
        }
    }
}
