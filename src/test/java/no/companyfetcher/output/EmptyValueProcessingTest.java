package no.companyfetcher.output;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EmptyValueProcessingTest {

    @Test
    void defaultsToUntouchedWhenMissing() {
        assertEquals(EmptyValueProcessing.UNTOUCHED, EmptyValueProcessing.parse(null));
        assertEquals(EmptyValueProcessing.UNTOUCHED, EmptyValueProcessing.parse(" "));
    }

    @Test
    void parsesConfiguredValues() {
        assertEquals(EmptyValueProcessing.CLEAR, EmptyValueProcessing.parse("clear"));
        assertEquals(EmptyValueProcessing.UNTOUCHED, EmptyValueProcessing.parse("UNTOUCHED"));
    }

    @Test
    void rejectsUnknownValues() {
        assertThrows(IllegalStateException.class, () -> EmptyValueProcessing.parse("DELETE"));
    }
}
