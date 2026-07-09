package no.companyfetcher.cli;

import no.companyfetcher.output.ExcelMergeOptions;
import no.companyfetcher.output.HighlightColor;

public final class CommandLineArgs {

    private CommandLineArgs() {
    }

    public static boolean isMergeExcelMode(String[] args) {
        return containsArg(args, "--merge-excel");
    }

    private static boolean containsArg(String[] args, String target) {
        for (String arg : args) {
            if (target.equals(arg)) {
                return true;
            }
        }
        return false;
    }

    public static ExcelMergeOptions parseMergeOptions(String[] args, String jarName) {
        boolean noHighlight = false;
        String colorArg = null;

        for (String arg : args) {
            if ("--no-highlight".equals(arg)) {
                noHighlight = true;
            } else if (arg.startsWith("--highlight-color=")) {
                colorArg = arg.substring("--highlight-color=".length());
            }
        }

        if (noHighlight) {
            return ExcelMergeOptions.withoutHighlight();
        }

        if (colorArg == null || colorArg.isBlank()) {
            throw new IllegalArgumentException("""
                    Missing --highlight-color for --merge-excel.
                    Example: java -jar %s --merge-excel --highlight-color=LIGHT_YELLOW
                    Use a different color on each run to see which cells were updated in the latest pass.
                    Available names: LIGHT_YELLOW, LIGHT_GREEN, LIGHT_BLUE, LIGHT_ORANGE, CORAL
                    Or use hex, e.g. --highlight-color=#FFF2CC
                    Add --no-highlight to update values without changing cell colors.""".formatted(jarName));
        }

        return ExcelMergeOptions.withHighlight(HighlightColor.parse(colorArg));
    }
}
