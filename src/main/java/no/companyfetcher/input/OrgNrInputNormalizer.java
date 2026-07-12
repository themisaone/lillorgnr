package no.companyfetcher.input;

import java.util.stream.Collectors;

public final class OrgNrInputNormalizer {

    private OrgNrInputNormalizer() {
    }

    public static String normalizeOrgNumber(String line) {
        return line.replaceAll("[\\s,]+", "");
    }

    public static String cleanFileContent(String content) {
        return content.lines()
                .map(line -> {
                    String trimmed = line.trim();
                    if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                        return line;
                    }
                    return normalizeOrgNumber(trimmed);
                })
                .collect(Collectors.joining(System.lineSeparator()));
    }
}
