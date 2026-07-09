package no.companyfetcher.output;

import java.util.Locale;
import java.util.Map;

public record HighlightColor(byte red, byte green, byte blue) {

    private static final Map<String, HighlightColor> NAMED = Map.of(
            "LIGHT_YELLOW", rgb(255, 242, 204),
            "LIGHT_GREEN", rgb(226, 239, 218),
            "LIGHT_BLUE", rgb(221, 235, 247),
            "LIGHT_ORANGE", rgb(252, 228, 214),
            "CORAL", rgb(248, 203, 173)
    );

    public static HighlightColor parse(String value) {
        if (value == null || value.isBlank()) {
            return defaultColor();
        }

        String normalized = value.trim().toUpperCase(Locale.ROOT);
        HighlightColor named = NAMED.get(normalized);
        if (named != null) {
            return named;
        }

        if (normalized.startsWith("#")) {
            normalized = normalized.substring(1);
        }

        if (normalized.length() != 6 || !normalized.matches("[0-9A-F]{6}")) {
            throw new IllegalArgumentException("Invalid highlight color: " + value
                    + ". Use a name like LIGHT_YELLOW or hex like #FFF2CC.");
        }

        return rgb(
                (byte) Integer.parseInt(normalized.substring(0, 2), 16),
                (byte) Integer.parseInt(normalized.substring(2, 4), 16),
                (byte) Integer.parseInt(normalized.substring(4, 6), 16)
        );
    }

    public static HighlightColor defaultColor() {
        return NAMED.get("LIGHT_YELLOW");
    }

    private static HighlightColor rgb(int red, int green, int blue) {
        return new HighlightColor((byte) red, (byte) green, (byte) blue);
    }
}
