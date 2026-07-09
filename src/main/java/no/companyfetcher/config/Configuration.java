package no.companyfetcher.config;

import no.companyfetcher.provider.ProviderType;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public class Configuration {

    private final Properties properties;

    public Configuration(Properties properties) {
        this.properties = properties;
    }

    public static Configuration load() {
        Properties properties = new Properties();

        try (InputStream defaults = Configuration.class.getClassLoader().getResourceAsStream("application.properties")) {
            if (defaults != null) {
                properties.load(defaults);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load default configuration", e);
        }

        Path localConfig = Path.of("config.properties");
        if (Files.exists(localConfig)) {
            try (InputStream local = Files.newInputStream(localConfig)) {
                properties.load(local);
            } catch (IOException e) {
                throw new IllegalStateException("Failed to load config.properties", e);
            }
        }

        return new Configuration(properties);
    }

    public int getAccountingYear() {
        return Integer.parseInt(require("accounting.year"));
    }

    public String getOutputFile() {
        return require("output.file");
    }

    public String getInputFile() {
        return require("input.file");
    }

    public long getRequestDelayMs() {
        return Long.parseLong(require("request.delay.ms"));
    }

    public int getRetryCount() {
        return Integer.parseInt(require("retry.count"));
    }

    public ProviderType getProviderType() {
        return ProviderType.valueOf(require("provider").trim().toUpperCase());
    }

    public String getProffApiKey() {
        String envKey = System.getenv("PROFF_API_KEY");
        if (envKey != null && !envKey.isBlank()) {
            return envKey.trim();
        }
        return properties.getProperty("proff.api.key", "").trim();
    }

    public String getExcelFile() {
        return require("excel.file");
    }

    public String getAquaOutputFile() {
        return require("aqua.output.file");
    }

    public String getFiskeridirApiBaseUrl() {
        return properties.getProperty("fiskeridir.api.base.url", "https://api.fiskeridir.no/pub-aqua").trim();
    }

    private String require(String key) {
        String value = properties.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing required configuration property: " + key);
        }
        return value.trim();
    }
}
