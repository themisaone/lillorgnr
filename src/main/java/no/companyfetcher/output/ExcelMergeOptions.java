package no.companyfetcher.output;

public record ExcelMergeOptions(
        boolean highlightUpdatedCells,
        HighlightColor highlightColor,
        EmptyValueProcessing emptyValueProcessing
) {

    public ExcelMergeOptions {
        if (emptyValueProcessing == null) {
            emptyValueProcessing = EmptyValueProcessing.UNTOUCHED;
        }
    }

    public static ExcelMergeOptions withoutHighlight() {
        return new ExcelMergeOptions(false, HighlightColor.defaultColor(), EmptyValueProcessing.UNTOUCHED);
    }

    public static ExcelMergeOptions withDefaultHighlight() {
        return new ExcelMergeOptions(true, HighlightColor.defaultColor(), EmptyValueProcessing.UNTOUCHED);
    }

    public static ExcelMergeOptions withHighlight(HighlightColor color) {
        return new ExcelMergeOptions(true, color, EmptyValueProcessing.UNTOUCHED);
    }

    public ExcelMergeOptions withEmptyValueProcessing(EmptyValueProcessing processing) {
        return new ExcelMergeOptions(highlightUpdatedCells, highlightColor, processing);
    }
}
