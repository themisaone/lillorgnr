package no.companyfetcher.config;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MtbLicenseFilterTest {

    @Test
    void parsesCommaSeparatedAllowedValues() {
        Set<String> values = MtbLicenseFilter.parseAllowedList(" matfisk , Kommersiell ");

        assertEquals(Set.of("matfisk", "kommersiell"), values);
    }

    @Test
    void matchesConfiguredProdStadiumAndFormal() throws Exception {
        var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        var license = mapper.readTree("""
                {
                  "type": {
                    "productionStageValue": "Matfisk",
                    "intentionValue": "KOMMERSIELL"
                  }
                }
                """);

        MtbLicenseFilter filter = MtbLicenseFilter.matfiskKommersiell();

        assertTrue(filter.allows(license));
    }

    @Test
    void rejectsForskningFormal() throws Exception {
        var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        var license = mapper.readTree("""
                {
                  "type": {
                    "productionStageValue": "Matfisk",
                    "intentionValue": "FORSKNING"
                  }
                }
                """);

        assertFalse(MtbLicenseFilter.matfiskKommersiell().allows(license));
    }
}
