package no.companyfetcher.output;

import no.companyfetcher.model.CompanyData;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ExcelUpdaterTest {

    @TempDir
    Path tempDir;

    @Test
    void updatesMatchingOrgNumbersInColumnsEgi() throws Exception {
        Path workbookPath = tempDir.resolve("companies.xlsx");
        createSampleWorkbook(workbookPath);

        Map<String, CompanyData> companies = Map.of(
                "994613405", new CompanyData("994613405", "Arnøy Laks AS", 2024, 248_204_000L, 25_194_000L, 15_303_000L, "OK"),
                "895366722", new CompanyData("895366722", "Arnøy Laks Slakteri AS", 2024, 93_284_000L, 25_007_000L, 5_077_000L, "OK")
        );

        ExcelUpdater updater = new ExcelUpdater(workbookPath, ExcelMergeOptions.withDefaultHighlight());
        ExcelUpdater.MergeResult result = updater.update(companies);

        assertEquals(2, result.updatedRows());
        assertEquals(true, result.highlighted());

        try (var workbook = new XSSFWorkbook(workbookPath.toFile())) {
            Row row6 = workbook.getSheetAt(0).getRow(5);
            assertEquals(248_204_000.0, row6.getCell(4).getNumericCellValue());
            assertEquals(FillPatternType.SOLID_FOREGROUND, row6.getCell(4).getCellStyle().getFillPattern());

            Row row7 = workbook.getSheetAt(0).getRow(6);
            assertEquals(93_284_000.0, row7.getCell(4).getNumericCellValue());
        }
    }

    @Test
    void reRunLeavesOldColorWhenRowIsNotUpdated() throws Exception {
        Path workbookPath = tempDir.resolve("companies.xlsx");
        createSampleWorkbook(workbookPath);

        Map<String, CompanyData> firstRun = Map.of(
                "994613405", new CompanyData("994613405", "Arnøy Laks AS", 2024, 100L, 200L, 300L, "OK"),
                "895366722", new CompanyData("895366722", "Arnøy Laks Slakteri AS", 2024, 400L, 500L, 600L, "OK")
        );
        new ExcelUpdater(workbookPath, ExcelMergeOptions.withHighlight(HighlightColor.parse("LIGHT_YELLOW")))
                .update(firstRun);

        short row7ColorAfterFirstRun;
        try (var workbook = new XSSFWorkbook(workbookPath.toFile())) {
            row7ColorAfterFirstRun = ((XSSFCellStyle) workbook.getSheetAt(0).getRow(6).getCell(4).getCellStyle())
                    .getFillForegroundColorColor().getIndexed();
        }

        Map<String, CompanyData> secondRun = Map.of(
                "994613405", new CompanyData("994613405", "Arnøy Laks AS", 2024, 999L, 888L, 777L, "OK")
        );
        new ExcelUpdater(workbookPath, ExcelMergeOptions.withHighlight(HighlightColor.parse("LIGHT_BLUE")))
                .update(secondRun);

        try (var workbook = new XSSFWorkbook(workbookPath.toFile())) {
            var updatedStyle = (XSSFCellStyle) workbook.getSheetAt(0).getRow(5).getCell(4).getCellStyle();
            var skippedStyle = (XSSFCellStyle) workbook.getSheetAt(0).getRow(6).getCell(4).getCellStyle();

            assertEquals(999.0, workbook.getSheetAt(0).getRow(5).getCell(4).getNumericCellValue());
            assertEquals(row7ColorAfterFirstRun, skippedStyle.getFillForegroundColorColor().getIndexed());
            assertArrayEquals(
                    new byte[]{(byte) 221, (byte) 235, (byte) 247},
                    updatedStyle.getFillForegroundColorColor().getRGB()
            );
        }
    }

    @Test
    void doesNotChangeUnrelatedCellsInSameColumns() throws Exception {
        Path workbookPath = tempDir.resolve("companies.xlsx");
        short untouchedStyleIndex;
        try (var workbook = new XSSFWorkbook()) {
            var sheet = workbook.createSheet("Sheet1");
            var row6 = sheet.createRow(5);
            row6.createCell(1).setCellValue("994613405");
            row6.createCell(4).setCellValue(1);
            var untouchedRow = sheet.createRow(10);
            untouchedRow.createCell(1).setCellValue("123456789");
            var untouchedStyle = workbook.createCellStyle();
            untouchedStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            var untouchedCell = untouchedRow.createCell(4);
            untouchedCell.setCellValue(999);
            untouchedCell.setCellStyle(untouchedStyle);
            untouchedStyleIndex = untouchedStyle.getIndex();

            try (var out = Files.newOutputStream(workbookPath)) {
                workbook.write(out);
            }
        }

        Map<String, CompanyData> companies = Map.of(
                "994613405", new CompanyData("994613405", "Arnøy Laks AS", 2024, 100L, 200L, 300L, "OK")
        );

        new ExcelUpdater(workbookPath, ExcelMergeOptions.withDefaultHighlight()).update(companies);

        try (var workbook = new XSSFWorkbook(workbookPath.toFile())) {
            short styleIndex = workbook.getSheetAt(0).getRow(10).getCell(4).getCellStyle().getIndex();
            assertEquals(untouchedStyleIndex, styleIndex);
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
