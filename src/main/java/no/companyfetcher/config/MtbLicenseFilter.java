package no.companyfetcher.config;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

public record MtbLicenseFilter(Set<String> allowedProdStadiums, Set<String> allowedFormals) {

    public static MtbLicenseFilter fromConfiguration(Configuration configuration) {
        return new MtbLicenseFilter(
                configuration.getMtbProdStadiumAllowed(),
                configuration.getMtbFormalAllowed()
        );
    }

    public static MtbLicenseFilter matfiskKommersiell() {
        return new MtbLicenseFilter(
                Set.of("matfisk"),
                Set.of("kommersiell")
        );
    }

    public static Set<String> parseAllowedList(String value) {
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(part -> !part.isEmpty())
                .map(part -> part.toLowerCase(Locale.ROOT))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public boolean allows(JsonNode license) {
        JsonNode type = license.path("type");
        String prodStadium = type.path("productionStageValue").asText("");
        String formal = type.path("intentionValue").asText("");
        return matchesAllowed(prodStadium, allowedProdStadiums)
                && matchesAllowed(formal, allowedFormals);
    }

    private static boolean matchesAllowed(String value, Set<String> allowed) {
        if (value == null || value.isBlank() || allowed.isEmpty()) {
            return false;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return allowed.contains(normalized);
    }
}
