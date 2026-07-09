package no.companyfetcher.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.companyfetcher.model.AquacultureCapacityData;
import no.companyfetcher.provider.ProviderException;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FiskeridirParserTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private final FiskeridirParser parser = new FiskeridirParser();

    @Test
    void sumsKapasitetFromAllLicenses() throws Exception {
        JsonNode entity = readFixture("fiskeridir/entity-994613405.json");
        JsonNode licenses = readFixture("fiskeridir/licenses-994613405.json");

        AquacultureCapacityData data = parser.parse("994613405", entity, licenses);

        assertEquals("994613405", data.orgNumber());
        assertEquals("ARNØY LAKS AS", data.companyName());
        assertEquals(4250.0, data.totalCapacity());
        assertEquals("TN", data.unit());
        assertEquals(4, data.entryCount());
        assertEquals("OK", data.status());
    }

    @Test
    void failsWhenEntityMissing() throws Exception {
        JsonNode licenses = readFixture("fiskeridir/licenses-994613405.json");

        assertThrows(ProviderException.class, () -> parser.parse("994613405", OBJECT_MAPPER.createArrayNode(), licenses));
    }

    private JsonNode readFixture(String resourcePath) throws Exception {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            return OBJECT_MAPPER.readTree(input);
        }
    }
}
