package no.companyfetcher.input;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MtbInputReaderTest {

    @TempDir
    Path tempDir;

    @Test
    void readsNameAndMtbPairs() throws Exception {
        Path input = tempDir.resolve("MtbInput.txt");
        Files.writeString(input, """
                # comment
                ARNØY LAKS,4250
                ELVEVOLL SETTEFISK,
                """);

        var rows = new MtbInputReader(input).read();

        assertEquals(2, rows.size());
        assertEquals("ARNØY LAKS", rows.get(0).name());
        assertEquals(4250.0, rows.get(0).mtb());
        assertEquals("ELVEVOLL SETTEFISK", rows.get(1).name());
        assertEquals(null, rows.get(1).mtb());
    }
}
