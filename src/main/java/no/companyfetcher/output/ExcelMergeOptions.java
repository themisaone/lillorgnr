package no.companyfetcher.output;

public record ExcelMergeOptions(boolean highlightUpdatedCells, HighlightColor highlightColor) {

    public static ExcelMergeOptions withoutHighlight() {
        return new ExcelMergeOptions(false, HighlightColor.defaultColor());
    }

    public static ExcelMergeOptions withDefaultHighlight() {
        return new ExcelMergeOptions(true, HighlightColor.defaultColor());
    }

    public static ExcelMergeOptions withHighlight(HighlightColor color) {
        return new ExcelMergeOptions(true, color);
    }
}
