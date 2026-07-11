package no.companyfetcher.parser;

import com.fasterxml.jackson.databind.JsonNode;
import no.companyfetcher.config.MtbLicenseFilter;
import no.companyfetcher.model.AquacultureCapacityData;
import no.companyfetcher.provider.ProviderException;

import java.util.LinkedHashSet;
import java.util.Set;

public class FiskeridirParser {

    private final MtbLicenseFilter filter;

    public FiskeridirParser(MtbLicenseFilter filter) {
        this.filter = filter;
    }

    public AquacultureCapacityData parse(String orgNumber, JsonNode entity, JsonNode licenses) {
        if (entity == null || !entity.isArray() || entity.isEmpty()) {
            throw new ProviderException(
                    ProviderException.Reason.NOT_FOUND,
                    "No legal entity found in Akvakulturregisteret for org number " + orgNumber
            );
        }

        String companyName = entity.get(0).path("name").asText(null);
        double total = 0.0;
        int entryCount = 0;
        Set<String> units = new LinkedHashSet<>();

        if (licenses != null && licenses.isArray()) {
            for (JsonNode license : licenses) {
                if (!filter.allows(license)) {
                    continue;
                }

                JsonNode capacity = license.path("capacity");
                if (capacity.isMissingNode() || capacity.isNull()) {
                    continue;
                }

                JsonNode current = capacity.path("current");
                if (current.isMissingNode() || current.isNull()) {
                    continue;
                }

                total += current.asDouble();
                entryCount++;

                String unit = capacity.path("unit").asText(null);
                if (unit != null && !unit.isBlank()) {
                    units.add(unit);
                }
            }
        }

        if (entryCount == 0) {
            return new AquacultureCapacityData(
                    orgNumber,
                    companyName,
                    null,
                    null,
                    0,
                    "OK: NO_MATCHING_MTB"
            );
        }

        String unit = units.size() == 1 ? units.iterator().next() : "MIXED";
        String status = units.size() <= 1 ? "OK" : "PARTIAL: MIXED_UNITS";

        return new AquacultureCapacityData(
                orgNumber,
                companyName,
                total,
                unit,
                entryCount,
                status
        );
    }
}
