package no.companyfetcher.cli;

import no.companyfetcher.output.ExcelMergeOptions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommandLineArgsTest {

    @Test
    void noArgsMeansGuiMode() {
        assertTrue(CommandLineArgs.isGuiMode(new String[]{}));
    }

    @Test
    void parsesCommand() {
        assertEquals(CommandLineArgs.Command.FETCH, CommandLineArgs.parseCommand(new String[]{"--command=FETCH"}));
        assertEquals(CommandLineArgs.Command.PROF, CommandLineArgs.parseCommand(new String[]{"--command=PROF"}));
        assertEquals(CommandLineArgs.Command.AQUA, CommandLineArgs.parseCommand(new String[]{"--command=aqua"}));
        assertEquals(CommandLineArgs.Command.MTB, CommandLineArgs.parseCommand(new String[]{"--command=MTB"}));
        assertNull(CommandLineArgs.parseCommand(new String[]{"--merge-excel"}));
        assertFalse(CommandLineArgs.isGuiMode(new String[]{"--command=FETCH"}));
    }

    @Test
    void requiresHighlightColorForMerge() {
        assertThrows(IllegalArgumentException.class, () ->
                CommandLineArgs.parseMergeOptions(new String[]{"--merge-excel"}));
    }

    @Test
    void acceptsHighlightColorForMerge() {
        ExcelMergeOptions options = CommandLineArgs.parseMergeOptions(new String[]{
                "--merge-excel",
                "--highlight-color=LIGHT_GREEN"
        });

        assertTrue(options.highlightUpdatedCells());
    }

    @Test
    void allowsNoHighlightOptOut() {
        ExcelMergeOptions options = CommandLineArgs.parseMergeOptions(new String[]{
                "--merge-excel",
                "--no-highlight"
        });

        assertFalse(options.highlightUpdatedCells());
    }
}
