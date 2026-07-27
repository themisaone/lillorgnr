package no.companyfetcher.output;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

final class ExcelWorkbookSupport {

    private ExcelWorkbookSupport() {
    }

    static void prepareForSave(Workbook workbook) {
        workbook.setForceFormulaRecalculation(true);
        markFormulaCellsStale(workbook);
    }

    private static void markFormulaCellsStale(Workbook workbook) {
        for (int sheetIndex = 0; sheetIndex < workbook.getNumberOfSheets(); sheetIndex++) {
            Sheet sheet = workbook.getSheetAt(sheetIndex);
            for (Row row : sheet) {
                if (row == null) {
                    continue;
                }
                for (Cell cell : row) {
                    if (cell != null && cell.getCellType() == CellType.FORMULA) {
                        cell.setCellFormula(cell.getCellFormula());
                    }
                }
            }
        }
    }
}
