package no.companyfetcher.output;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ExcelMergeOptionsEmptyValueProcessingTest {

    @Test
    void defaultsToUntouchedForAllFactories() {
        assertEquals(EmptyValueProcessing.UNTOUCHED, ExcelMergeOptions.withoutHighlight().emptyValueProcessing());
        assertEquals(EmptyValueProcessing.UNTOUCHED, ExcelMergeOptions.withDefaultHighlight().emptyValueProcessing());
        assertEquals(
                EmptyValueProcessing.UNTOUCHED,
                ExcelMergeOptions.withHighlight(HighlightColor.parse("LIGHT_GREEN")).emptyValueProcessing()
        );
    }

    @Test
    void withEmptyValueProcessingCreatesNewOptionsInstance() {
        ExcelMergeOptions original = ExcelMergeOptions.withDefaultHighlight();
        ExcelMergeOptions cleared = original.withEmptyValueProcessing(EmptyValueProcessing.CLEAR);

        assertEquals(EmptyValueProcessing.UNTOUCHED, original.emptyValueProcessing());
        assertEquals(EmptyValueProcessing.CLEAR, cleared.emptyValueProcessing());
        assertEquals(original.highlightColor(), cleared.highlightColor());
        assertEquals(original.highlightUpdatedCells(), cleared.highlightUpdatedCells());
    }
}
