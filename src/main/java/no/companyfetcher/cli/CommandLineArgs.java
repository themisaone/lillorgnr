package no.companyfetcher.cli;

import no.companyfetcher.output.ExcelMergeOptions;
import no.companyfetcher.output.HighlightColor;

public final class CommandLineArgs {

    public static final String JAR_NAME = "OrgNrGui.jar";

    public enum Command {
        FETCH,
        PROF,
        AQUA,
        MTB
    }

    private CommandLineArgs() {
    }

    public static boolean isGuiMode(String[] args) {
        return args.length == 0 || parseCommand(args) == null;
    }

    public static Command parseCommand(String[] args) {
        for (String arg : args) {
            if (arg.startsWith("--command=")) {
                String value = arg.substring("--command=".length()).trim().toUpperCase();
                return Command.valueOf(value);
            }
        }
        return null;
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

    public static ExcelMergeOptions parseMergeOptions(String[] args) {
        return parseMergeOptions(args, JAR_NAME);
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
                    Example: java -jar %s --command=PROF --merge-excel --highlight-color=LIGHT_YELLOW
                    Use a different color on each run to see which cells were updated in the latest pass.
                    Available names: LIGHT_YELLOW, LIGHT_GREEN, LIGHT_BLUE, LIGHT_ORANGE, CORAL
                    Or use hex, e.g. --highlight-color=#FFF2CC
                    Add --no-highlight to update values without changing cell colors.""".formatted(jarName));
        }

        return ExcelMergeOptions.withHighlight(HighlightColor.parse(colorArg));
    }

    public static String usage() {
        return """
                Usage:
                  java -jar %s
                  java -jar %s --command=FETCH
                  java -jar %s --command=PROF --merge-excel --highlight-color=LIGHT_YELLOW
                  java -jar %s --command=AQUA --merge-excel --highlight-color=LIGHT_BLUE
                  java -jar %s --command=MTB --merge-excel --highlight-color=LIGHT_GREEN
                
                Commands: FETCH, PROF/AQUA/MTB (merge only)
                No arguments opens the GUI.""".formatted(JAR_NAME, JAR_NAME, JAR_NAME, JAR_NAME, JAR_NAME);
    }
}
