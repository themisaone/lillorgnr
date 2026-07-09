package no.companyfetcher.parser;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ProffParserTest {

    private final ProffParser parser = new ProffParser();

    @Test
    void parseAmountConvertsThousandsToNok() {
        assertEquals(248_204_000L, parser.parseAmount("248 204"));
        assertEquals(25_194_000L, parser.parseAmount("25 194"));
        assertEquals(15_303_000L, parser.parseAmount("15 303"));
    }

    @Test
    void parseAccountingTableFromHtml() {
        String html = """
                <html><body>
                <h1>Arnøy Laks AS</h1>
                <table>
                  <tr><th>Regnskap</th><th>2024-12</th></tr>
                  <tr><td>Sum driftsinntekter</td><td>248 204</td></tr>
                  <tr><td>Lønnskostnader</td><td>25 194</td></tr>
                  <tr><td>Driftsresultat (EBIT)</td><td>15 303</td></tr>
                </table>
                </body></html>
                """;

        Document document = Jsoup.parse(html);
        var result = parser.parse("994613405", document, 2024);

        assertEquals("Arnøy Laks AS", result.companyName());
        assertEquals(248_204_000L, result.revenue());
        assertEquals(25_194_000L, result.salaryCost());
        assertEquals(15_303_000L, result.ebit());
        assertEquals("OK", result.status());
    }

    @Test
    void parsePartialWhenSalaryCostMissing() {
        String html = """
                <html><body>
                <h1>Example AS</h1>
                <table>
                  <tr><th>Regnskap</th><th>2024-12</th></tr>
                  <tr><td>Sum driftsinntekter</td><td>100</td></tr>
                  <tr><td>Driftsresultat (EBIT)</td><td>10</td></tr>
                </table>
                </body></html>
                """;

        Document document = Jsoup.parse(html);
        var result = parser.parse("123456789", document, 2024);

        assertEquals("PARTIAL", result.status());
        assertNull(result.salaryCost());
    }
}
