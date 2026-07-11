package no.companyfetcher.config;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class MtbStagesLoader {

    private MtbStagesLoader() {
    }

    public static List<MtbStage> load(Path stagesFile) {
        if (!Files.exists(stagesFile)) {
            throw new IllegalStateException("Missing MTB stages file: " + stagesFile.toAbsolutePath());
        }

        List<MtbStage> stages = new ArrayList<>();

        try (Reader reader = Files.newBufferedReader(stagesFile);
             CSVParser parser = CSVFormat.DEFAULT.builder()
                     .setHeader()
                     .setSkipHeaderRecord(true)
                     .setIgnoreHeaderCase(true)
                     .setTrim(true)
                     .build()
                     .parse(reader)) {

            for (CSVRecord record : parser) {
                stages.add(MtbStage.parse(
                        record.get("low"),
                        record.get("high"),
                        record.get("price")
                ));
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read MTB stages file: " + stagesFile, e);
        }

        if (stages.isEmpty()) {
            throw new IllegalStateException("MTB stages file is empty: " + stagesFile);
        }

        return List.copyOf(stages);
    }
}
