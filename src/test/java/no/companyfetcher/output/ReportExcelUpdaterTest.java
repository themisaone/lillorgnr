package no.companyfetcher.output;

import no.companyfetcher.model.OrgNrReportData;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

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
    void leavesEmptyProffFieldsUntouched() throws Exception {
        Path workbookPath = tempDir.resolve("companies.xlsx");
        short originalStyleIndex;
        try (var workbook = new XSSFWorkbook()) {
            var sheet = workbook.createSheet("Sheet1");
            var row = sheet.createRow(5);
            row.createCell(1).setCellValue("994613405");
            var cell = row.createCell(4);
            cell.setCellValue(123_456.0);
            var style = workbook.createCellStyle();
            style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            cell.setCellStyle(style);
            originalStyleIndex = style.getIndex();
            try (var out = Files.newOutputStream(workbookPath)) {
                workbook.write(out);
            }
        }

        Map<String, OrgNrReportData> rows = Map.of(
                "994613405", new OrgNrReportData(
                        "994613405", null, null,
                        null, null, null, "ERROR",
                        null, null, 0, null, null
                )
        );

        ReportExcelUpdater.MergeResult result = new ReportExcelUpdater(
                workbookPath,
                ExcelMergeOptions.withDefaultHighlight()
        ).update(rows, ReportExcelUpdater.Scope.PROF);

        assertEquals(0, result.updatedRows());

        try (var workbook = new XSSFWorkbook(workbookPath.toFile())) {
            var cell = workbook.getSheetAt(0).getRow(5).getCell(4);
            assertEquals(123_456.0, cell.getNumericCellValue());
            assertEquals(originalStyleIndex, cell.getCellStyle().getIndex());
        }
    }

    @Test
    void clearsEmptyProffFieldsWhenConfiguredToClear() throws Exception {
        Path workbookPath = tempDir.resolve("companies.xlsx");
        try (var workbook = new XSSFWorkbook()) {
            var sheet = workbook.createSheet("Sheet1");
            var row = sheet.createRow(5);
            row.createCell(1).setCellValue("994613405");
            row.createCell(4).setCellValue(123_456.0);
            try (var out = Files.newOutputStream(workbookPath)) {
                workbook.write(out);
            }
        }

        Map<String, OrgNrReportData> rows = Map.of(
                "994613405", new OrgNrReportData(
                        "994613405", null, null,
                        null, null, null, "ERROR",
                        null, null, 0, null, null
                )
        );

        ReportExcelUpdater.MergeResult result = new ReportExcelUpdater(
                workbookPath,
                ExcelMergeOptions.withDefaultHighlight().withEmptyValueProcessing(EmptyValueProcessing.CLEAR)
        ).update(rows, ReportExcelUpdater.Scope.PROF);

        assertEquals(1, result.updatedRows());

        try (var workbook = new XSSFWorkbook(workbookPath.toFile())) {
            var row = workbook.getSheetAt(0).getRow(5);
            assertEquals(CellType.BLANK, row.getCell(4).getCellType());
            assertEquals(CellType.BLANK, row.getCell(6).getCellType());
            assertEquals(CellType.BLANK, row.getCell(8).getCellType());
            assertEquals(FillPatternType.SOLID_FOREGROUND, row.getCell(4).getCellStyle().getFillPattern());
        }
    }

    @Test
    void updatesOnlyPresentFieldsWhenUntouched() throws Exception {
        Path workbookPath = tempDir.resolve("companies.xlsx");
        short originalSalaryStyleIndex;
        try (var workbook = new XSSFWorkbook()) {
            var sheet = workbook.createSheet("Sheet1");
            var row = sheet.createRow(5);
            row.createCell(1).setCellValue("994613405");
            row.createCell(6).setCellValue(222.0);
            var salaryStyle = workbook.createCellStyle();
            salaryStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            row.getCell(6).setCellStyle(salaryStyle);
            originalSalaryStyleIndex = salaryStyle.getIndex();
            try (var out = Files.newOutputStream(workbookPath)) {
                workbook.write(out);
            }
        }

        Map<String, OrgNrReportData> rows = Map.of(
                "994613405", new OrgNrReportData(
                        "994613405", "Arnøy Laks AS", 2024,
                        100L, null, null, "OK",
                        null, null, 0, null, null
                )
        );

        ReportExcelUpdater.MergeResult result = new ReportExcelUpdater(
                workbookPath,
                ExcelMergeOptions.withDefaultHighlight()
        ).update(rows, ReportExcelUpdater.Scope.PROF);

        assertEquals(1, result.updatedRows());

        try (var workbook = new XSSFWorkbook(workbookPath.toFile())) {
            var row = workbook.getSheetAt(0).getRow(5);
            assertEquals(100.0, row.getCell(4).getNumericCellValue());
            assertEquals(222.0, row.getCell(6).getNumericCellValue());
            assertEquals(originalSalaryStyleIndex, row.getCell(6).getCellStyle().getIndex());
            assertNull(row.getCell(8));
        }
    }

    @Test
    void clearsOnlyEmptyFieldsWhenConfiguredToClear() throws Exception {
        Path workbookPath = tempDir.resolve("companies.xlsx");
        try (var workbook = new XSSFWorkbook()) {
            var sheet = workbook.createSheet("Sheet1");
            var row = sheet.createRow(5);
            row.createCell(1).setCellValue("994613405");
            row.createCell(4).setCellValue(111.0);
            row.createCell(6).setCellValue(222.0);
            row.createCell(8).setCellValue(333.0);
            try (var out = Files.newOutputStream(workbookPath)) {
                workbook.write(out);
            }
        }

        Map<String, OrgNrReportData> rows = Map.of(
                "994613405", new OrgNrReportData(
                        "994613405", "Arnøy Laks AS", 2024,
                        100L, null, 300L, "OK",
                        null, null, 0, null, null
                )
        );

        new ReportExcelUpdater(
                workbookPath,
                ExcelMergeOptions.withDefaultHighlight().withEmptyValueProcessing(EmptyValueProcessing.CLEAR)
        ).update(rows, ReportExcelUpdater.Scope.PROF);

        try (var workbook = new XSSFWorkbook(workbookPath.toFile())) {
            var row = workbook.getSheetAt(0).getRow(5);
            assertEquals(100.0, row.getCell(4).getNumericCellValue());
            assertEquals(CellType.BLANK, row.getCell(6).getCellType());
            assertEquals(300.0, row.getCell(8).getNumericCellValue());
        }
    }

    @Test
    void leavesNullFeeUntouched() throws Exception {
        Path workbookPath = tempDir.resolve("companies.xlsx");
        short originalFeeStyleIndex;
        try (var workbook = new XSSFWorkbook()) {
            var sheet = workbook.createSheet("Sheet1");
            var row = sheet.createRow(5);
            row.createCell(1).setCellValue("994613405");
            var feeCell = row.createCell(12);
            feeCell.setCellValue(55_000.0);
            var feeStyle = workbook.createCellStyle();
            feeStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            feeCell.setCellStyle(feeStyle);
            originalFeeStyleIndex = feeStyle.getIndex();
            try (var out = Files.newOutputStream(workbookPath)) {
                workbook.write(out);
            }
        }

        Map<String, OrgNrReportData> rows = Map.of(
                "994613405", new OrgNrReportData(
                        "994613405", "Arnøy Laks AS", 2024,
                        null, null, null, "OK",
                        null, null, 0, "OK", null
                )
        );

        ReportExcelUpdater.MergeResult result = new ReportExcelUpdater(
                workbookPath,
                ExcelMergeOptions.withDefaultHighlight()
        ).update(rows, ReportExcelUpdater.Scope.FEE);

        assertEquals(0, result.updatedRows());

        try (var workbook = new XSSFWorkbook(workbookPath.toFile())) {
            var feeCell = workbook.getSheetAt(0).getRow(5).getCell(12);
            assertEquals(55_000.0, feeCell.getNumericCellValue());
            assertEquals(originalFeeStyleIndex, feeCell.getCellStyle().getIndex());
        }
    }

    @Test
    void clearsNullFeeWhenConfiguredToClear() throws Exception {
        Path workbookPath = tempDir.resolve("companies.xlsx");
        try (var workbook = new XSSFWorkbook()) {
            var sheet = workbook.createSheet("Sheet1");
            var row = sheet.createRow(5);
            row.createCell(1).setCellValue("994613405");
            row.createCell(12).setCellValue(55_000.0);
            try (var out = Files.newOutputStream(workbookPath)) {
                workbook.write(out);
            }
        }

        Map<String, OrgNrReportData> rows = Map.of(
                "994613405", new OrgNrReportData(
                        "994613405", "Arnøy Laks AS", 2024,
                        null, null, null, "OK",
                        null, null, 0, "OK", null
                )
        );

        ReportExcelUpdater.MergeResult result = new ReportExcelUpdater(
                workbookPath,
                ExcelMergeOptions.withDefaultHighlight().withEmptyValueProcessing(EmptyValueProcessing.CLEAR)
        ).update(rows, ReportExcelUpdater.Scope.FEE);

        assertEquals(1, result.updatedRows());

        try (var workbook = new XSSFWorkbook(workbookPath.toFile())) {
            var feeCell = workbook.getSheetAt(0).getRow(5).getCell(12);
            assertEquals(CellType.BLANK, feeCell.getCellType());
            assertEquals(FillPatternType.SOLID_FOREGROUND, feeCell.getCellStyle().getFillPattern());
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

    @Test
    void marksWorkbookForFormulaRecalculationOnOpen() throws Exception {
        Path workbookPath = tempDir.resolve("companies.xlsx");
        createSampleWorkbook(workbookPath);

        Map<String, OrgNrReportData> rows = Map.of(
                "994613405", new OrgNrReportData(
                        "994613405", "Arnøy Laks AS", 2024,
                        248_204_000L, 25_194_000L, 15_303_000L, "OK",
                        4250.0, "MTB", 4, "OK", 73_830L
                )
        );

        new ReportExcelUpdater(workbookPath, ExcelMergeOptions.withDefaultHighlight())
                .update(rows, ReportExcelUpdater.Scope.PROF);

        try (var workbook = new XSSFWorkbook(workbookPath.toFile())) {
            assertEquals(true, workbook.getForceFormulaRecalculation());
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
