package no.companyfetcher.service;

import no.companyfetcher.config.Configuration;
import no.companyfetcher.output.EmptyValueProcessing;
import no.companyfetcher.output.ExcelMergeOptions;
import no.companyfetcher.output.ReportExcelUpdater;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ReportExcelMergeServiceEmptyValueProcessingTest {

    private static final String ORG_NR = "994613405";

    @TempDir
    Path tempDir;

    @Test
    void usesUntouchedFromConfigurationByDefault() throws Exception {
        Path workbookPath = createWorkbookWithExistingValues();
        Path csvPath = writeCsvWithEmptyProffValues();
        Configuration configuration = configuration(csvPath, workbookPath, "UNTOUCHED");

        ReportExcelUpdater.MergeResult result = new ReportExcelMergeService().merge(
                configuration,
                ReportExcelUpdater.Scope.ALL,
                ExcelMergeOptions.withDefaultHighlight()
        );

        assertEquals(0, result.updatedRows());

        try (var workbook = new XSSFWorkbook(workbookPath.toFile())) {
            var row = workbook.getSheetAt(0).getRow(5);
            assertEquals(111.0, row.getCell(4).getNumericCellValue());
            assertEquals(222.0, row.getCell(6).getNumericCellValue());
            assertEquals(333.0, row.getCell(8).getNumericCellValue());
            assertEquals(444.0, row.getCell(10).getNumericCellValue());
            assertEquals(555.0, row.getCell(12).getNumericCellValue());
        }
    }

    @Test
    void usesClearFromConfiguration() throws Exception {
        Path workbookPath = createWorkbookWithExistingValues();
        Path csvPath = writeCsvWithEmptyProffValues();
        Configuration configuration = configuration(csvPath, workbookPath, "CLEAR");

        ReportExcelUpdater.MergeResult result = new ReportExcelMergeService().merge(
                configuration,
                ReportExcelUpdater.Scope.ALL,
                ExcelMergeOptions.withDefaultHighlight()
        );

        assertEquals(1, result.updatedRows());

        try (var workbook = new XSSFWorkbook(workbookPath.toFile())) {
            var row = workbook.getSheetAt(0).getRow(5);
            assertEquals(CellType.BLANK, row.getCell(4).getCellType());
            assertEquals(CellType.BLANK, row.getCell(6).getCellType());
            assertEquals(CellType.BLANK, row.getCell(8).getCellType());
            assertEquals(CellType.BLANK, row.getCell(10).getCellType());
            assertEquals(CellType.BLANK, row.getCell(12).getCellType());
            assertEquals(FillPatternType.SOLID_FOREGROUND, row.getCell(4).getCellStyle().getFillPattern());
        }
    }

    @Test
    void configurationOverridesOptionsWhenModesDiffer() throws Exception {
        Path workbookPath = createWorkbookWithExistingValues();
        Path csvPath = writeCsvWithEmptyProffValues();
        Configuration configuration = configuration(csvPath, workbookPath, "CLEAR");

        ReportExcelUpdater.MergeResult result = new ReportExcelMergeService().merge(
                configuration,
                ReportExcelUpdater.Scope.PROF,
                ExcelMergeOptions.withDefaultHighlight().withEmptyValueProcessing(EmptyValueProcessing.UNTOUCHED)
        );

        assertEquals(1, result.updatedRows());

        try (var workbook = new XSSFWorkbook(workbookPath.toFile())) {
            var row = workbook.getSheetAt(0).getRow(5);
            assertEquals(CellType.BLANK, row.getCell(4).getCellType());
            assertEquals(CellType.BLANK, row.getCell(6).getCellType());
            assertEquals(CellType.BLANK, row.getCell(8).getCellType());
            assertEquals(444.0, row.getCell(10).getNumericCellValue());
            assertEquals(555.0, row.getCell(12).getNumericCellValue());
        }
    }

    private Configuration configuration(Path csvPath, Path workbookPath, String emptyValueProcessing) {
        Properties properties = new Properties();
        properties.setProperty("output.file", csvPath.toString());
        properties.setProperty("excel.file", workbookPath.toString());
        properties.setProperty("empty.value.processing", emptyValueProcessing);
        return new Configuration(properties, new Properties());
    }

    private Path createWorkbookWithExistingValues() throws Exception {
        Path workbookPath = tempDir.resolve("companies.xlsx");
        try (var workbook = new XSSFWorkbook()) {
            var sheet = workbook.createSheet("Sheet1");
            var row = sheet.createRow(5);
            row.createCell(1).setCellValue(ORG_NR);
            row.createCell(4).setCellValue(111);
            row.createCell(6).setCellValue(222);
            row.createCell(8).setCellValue(333);
            row.createCell(10).setCellValue(444);
            row.createCell(12).setCellValue(555);
            try (var out = Files.newOutputStream(workbookPath)) {
                workbook.write(out);
            }
        }
        return workbookPath;
    }

    private Path writeCsvWithEmptyProffValues() throws Exception {
        Path csvPath = tempDir.resolve("OrgNrReport.csv");
        Files.writeString(csvPath, """
                OrgNr,OrgName,AccountingYear,Revenue,SalaryCost,EBIT,ProffStatus,TotalCapacity,Unit,EntryCount,AquaStatus,Fee
                %s,Test AS,,,,,ERROR,,,0,, 
                """.formatted(ORG_NR));
        return csvPath;
    }
}
