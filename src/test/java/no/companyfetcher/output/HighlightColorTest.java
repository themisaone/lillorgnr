package no.companyfetcher.output;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HighlightColorTest {

    @Test
    void parsesNamedColor() {
        HighlightColor color = HighlightColor.parse("LIGHT_YELLOW");
        assertEquals((byte) 255, color.red());
        assertEquals((byte) 242, color.green());
        assertEquals((byte) 204, color.blue());
    }

    @Test
    void parsesHexColor() {
        HighlightColor color = HighlightColor.parse("#E2EFDA");
        assertEquals((byte) 226, color.red());
        assertEquals((byte) 239, color.green());
        assertEquals((byte) 218, color.blue());
    }
}
