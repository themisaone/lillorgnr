package no.companyfetcher.input;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

public class TextFileReader implements CompanyInputReader {

    private static final Logger log = LoggerFactory.getLogger(TextFileReader.class);

    private final Path inputFile;

    public TextFileReader(Path inputFile) {
        this.inputFile = inputFile;
    }

    @Override
    public List<String> read() {
        if (!Files.exists(inputFile)) {
            throw new IllegalStateException("Input file not found: " + inputFile.toAbsolutePath());
        }

        try {
            List<String> orgNumbers = Files.lines(inputFile)
                    .map(String::trim)
                    .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                    .map(line -> line.replaceAll("\\s+", ""))
                    .collect(Collectors.toList());

            log.info("Read {} organization numbers from {}", orgNumbers.size(), inputFile.getFileName());
            return orgNumbers;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read input file: " + inputFile, e);
        }
    }
}
