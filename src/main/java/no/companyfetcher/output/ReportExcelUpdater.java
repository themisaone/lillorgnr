package no.companyfetcher.output;

import no.companyfetcher.model.OrgNrReportData;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class ReportExcelUpdater {

    private static final Logger log = LoggerFactory.getLogger(ReportExcelUpdater.class);

    private static final int COL_ORG_NR = 1;           // B
    private static final int COL_REVENUE = 4;          // E
    private static final int COL_SALARY = 6;           // G
    private static final int COL_EBIT = 8;             // I
    private static final int COL_TOTAL_CAPACITY = 10;  // K
    private static final int COL_FEE = 12;             // M

    public enum Scope {
        PROF,
        AQUA,
        FEE,
        ALL
    }

    private final Path excelFile;
    private final ExcelMergeOptions options;

    public ReportExcelUpdater(Path excelFile, ExcelMergeOptions options) {
        this.excelFile = excelFile;
        this.options = options == null ? ExcelMergeOptions.withoutHighlight() : options;
    }

    public MergeResult update(Map<String, OrgNrReportData> rowsByOrgNumber, Scope scope) {
        if (!Files.exists(excelFile)) {
            throw new IllegalStateException("Excel file not found: " + excelFile.toAbsolutePath());
        }

        try (InputStream input = Files.newInputStream(excelFile);
             Workbook workbook = WorkbookFactory.create(input)) {

            Sheet sheet = workbook.getSheetAt(0);
            StyleCache styleCache = new StyleCache(workbook, options);

            int updatedRows = 0;
            int skippedRows = 0;

            for (Row row : sheet) {
                if (row == null) {
                    continue;
                }

                String orgNumber = readOrgNumber(row.getCell(COL_ORG_NR));
                if (orgNumber == null) {
                    continue;
                }

                OrgNrReportData rowData = rowsByOrgNumber.get(orgNumber);
                if (rowData == null) {
                    skippedRows++;
                    log.debug("No CSV data for org number {} on row {}", orgNumber, row.getRowNum() + 1);
                    continue;
                }

                applyScope(row, rowData, scope, styleCache);
                updatedRows++;
                log.info("Updated row {} ({}) with {}", row.getRowNum() + 1, orgNumber, scope);
            }

            try (OutputStream output = Files.newOutputStream(excelFile)) {
                workbook.write(output);
            }

            log.info("Updated {} rows in {} ({})", updatedRows, excelFile.toAbsolutePath(), scope);
            return new MergeResult(updatedRows, skippedRows, options.highlightUpdatedCells());
        } catch (IOException e) {
            throw new IllegalStateException("Failed to update Excel file: " + excelFile, e);
        }
    }

    private void applyScope(Row row, OrgNrReportData data, Scope scope, StyleCache styleCache) {
        switch (scope) {
            case PROF -> writeProff(row, data, styleCache);
            case AQUA -> writeAqua(row, data, styleCache);
            case FEE -> writeFee(row, data, styleCache);
            case ALL -> {
                writeProff(row, data, styleCache);
                writeAqua(row, data, styleCache);
                writeFee(row, data, styleCache);
            }
        }
    }

    private void writeProff(Row row, OrgNrReportData data, StyleCache styleCache) {
        writeLongCell(row, COL_REVENUE, data.revenue(), styleCache);
        writeLongCell(row, COL_SALARY, data.salaryCost(), styleCache);
        writeLongCell(row, COL_EBIT, data.ebit(), styleCache);
    }

    private void writeAqua(Row row, OrgNrReportData data, StyleCache styleCache) {
        writeDoubleCell(row, COL_TOTAL_CAPACITY, data.totalCapacity(), styleCache);
    }

    private void writeFee(Row row, OrgNrReportData data, StyleCache styleCache) {
        if (data.fee() != null) {
            writeLongCell(row, COL_FEE, data.fee(), styleCache);
        }
    }

    private void writeLongCell(Row row, int columnIndex, Long value, StyleCache styleCache) {
        Cell cell = getOrCreateCell(row, columnIndex);
        if (value == null) {
            cell.setBlank();
        } else {
            cell.setCellValue(value.doubleValue());
        }
        applyHighlight(cell, styleCache);
    }

    private void writeDoubleCell(Row row, int columnIndex, Double value, StyleCache styleCache) {
        Cell cell = getOrCreateCell(row, columnIndex);
        if (value == null) {
            cell.setBlank();
        } else {
            cell.setCellValue(value);
        }
        applyHighlight(cell, styleCache);
    }

    private Cell getOrCreateCell(Row row, int columnIndex) {
        Cell cell = row.getCell(columnIndex);
        return cell == null ? row.createCell(columnIndex) : cell;
    }

    private void applyHighlight(Cell cell, StyleCache styleCache) {
        if (options.highlightUpdatedCells()) {
            cell.setCellStyle(styleCache.withHighlight(cell.getCellStyle()));
        }
    }

    private String readOrgNumber(Cell cell) {
        if (cell == null) {
            return null;
        }

        if (cell.getCellType() == CellType.NUMERIC) {
            return normalizeOrgNumber(String.valueOf((long) cell.getNumericCellValue()));
        }

        if (cell.getCellType() == CellType.STRING) {
            return normalizeOrgNumber(cell.getStringCellValue());
        }

        if (cell.getCellType() == CellType.FORMULA) {
            return switch (cell.getCachedFormulaResultType()) {
                case NUMERIC -> normalizeOrgNumber(String.valueOf((long) cell.getNumericCellValue()));
                case STRING -> normalizeOrgNumber(cell.getStringCellValue());
                default -> null;
            };
        }

        return null;
    }

    private String normalizeOrgNumber(String value) {
        if (value == null) {
            return null;
        }
        String digits = value.replaceAll("\\D", "");
        return digits.length() == 9 ? digits : null;
    }

    public record MergeResult(int updatedRows, int skippedRows, boolean highlighted) {
    }

    private static final class StyleCache {

        private final Workbook workbook;
        private final ExcelMergeOptions options;
        private final Map<Short, CellStyle> highlightedByBase = new HashMap<>();
        private CellStyle defaultHighlighted;

        private StyleCache(Workbook workbook, ExcelMergeOptions options) {
            this.workbook = workbook;
            this.options = options;
        }

        private CellStyle withHighlight(CellStyle baseStyle) {
            if (baseStyle == null) {
                if (defaultHighlighted == null) {
                    defaultHighlighted = createHighlightedStyle(null);
                }
                return defaultHighlighted;
            }

            return highlightedByBase.computeIfAbsent(baseStyle.getIndex(), index -> createHighlightedStyle(baseStyle));
        }

        private CellStyle createHighlightedStyle(CellStyle baseStyle) {
            CellStyle style = workbook.createCellStyle();
            if (baseStyle != null) {
                style.cloneStyleFrom(baseStyle);
            }
            applyFill(style, options.highlightColor());
            return style;
        }

        private void applyFill(CellStyle style, HighlightColor color) {
            if (workbook instanceof XSSFWorkbook && style instanceof XSSFCellStyle xssfStyle) {
                xssfStyle.setFillForegroundColor(new XSSFColor(
                        new byte[]{color.red(), color.green(), color.blue()},
                        null
                ));
            }
            style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        }
    }
}
