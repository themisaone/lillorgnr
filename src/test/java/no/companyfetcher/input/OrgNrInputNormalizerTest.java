package no.companyfetcher.input;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OrgNrInputNormalizerTest {

    @Test
    void removesCommasAndWhitespaceFromOrgNumber() {
        assertEquals("930155101", OrgNrInputNormalizer.normalizeOrgNumber("930,155,101"));
        assertEquals("930155101", OrgNrInputNormalizer.normalizeOrgNumber("930 155 101"));
        assertEquals("981160207", OrgNrInputNormalizer.normalizeOrgNumber("981160207"));
    }

    @Test
    void cleansFileContentButKeepsComments() {
        String input = """
                # Kommentar
                930,155,101
                981 160 207
                
                # Slutt
                """;

        String cleaned = OrgNrInputNormalizer.cleanFileContent(input);

        assertEquals("""
                # Kommentar
                930155101
                981160207
                
                # Slutt
                """.trim(), cleaned.trim());
    }
}
