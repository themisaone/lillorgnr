package no.companyfetcher.input;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class MtbInputReader {

    private static final Logger log = LoggerFactory.getLogger(MtbInputReader.class);

    private final Path inputFile;

    public MtbInputReader(Path inputFile) {
        this.inputFile = inputFile;
    }

    public List<MtbInputRow> read() {
        if (!Files.exists(inputFile)) {
            throw new IllegalStateException("MTB input file not found: " + inputFile.toAbsolutePath());
        }

        List<MtbInputRow> rows = new ArrayList<>();

        try {
            for (String line : Files.readAllLines(inputFile)) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }

                String[] parts = trimmed.split(",", 2);
                if (parts.length < 2) {
                    throw new IllegalStateException("Invalid MTB input line (expected name,mtb): " + line);
                }

                String name = parts[0].trim();
                String mtbRaw = parts[1].trim();
                Double mtb = mtbRaw.isEmpty() ? null : Double.parseDouble(mtbRaw.replaceAll("\\s+", ""));
                rows.add(new MtbInputRow(name, mtb));
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read MTB input file: " + inputFile, e);
        }

        log.info("Read {} MTB rows from {}", rows.size(), inputFile.getFileName());
        return rows;
    }

    public record MtbInputRow(String name, Double mtb) {
    }
}
