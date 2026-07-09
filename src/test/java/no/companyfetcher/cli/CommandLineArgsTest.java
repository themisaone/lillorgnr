package no.companyfetcher.cli;

import no.companyfetcher.output.ExcelMergeOptions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommandLineArgsTest {

    @Test
    void requiresHighlightColorForMerge() {
        assertThrows(IllegalArgumentException.class, () ->
                CommandLineArgs.parseMergeOptions(new String[]{"--merge-excel"}, "CompanyFetcher.jar"));
    }

    @Test
    void acceptsHighlightColorForMerge() {
        ExcelMergeOptions options = CommandLineArgs.parseMergeOptions(new String[]{
                "--merge-excel",
                "--highlight-color=LIGHT_GREEN"
        }, "CompanyFetcher.jar");

        assertTrue(options.highlightUpdatedCells());
    }

    @Test
    void allowsNoHighlightOptOut() {
        ExcelMergeOptions options = CommandLineArgs.parseMergeOptions(new String[]{
                "--merge-excel",
                "--no-highlight"
        }, "CompanyFetcher.jar");

        assertFalse(options.highlightUpdatedCells());
    }
}
