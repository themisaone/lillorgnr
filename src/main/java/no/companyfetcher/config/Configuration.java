package no.companyfetcher.config;

import no.companyfetcher.provider.ProviderType;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.Set;

public class Configuration {

    private final Properties properties;
    private final Properties accountingInfo;

    public Configuration(Properties properties, Properties accountingInfo) {
        this.properties = properties;
        this.accountingInfo = accountingInfo;
    }

    public static Configuration load() {
        Path localConfig = Path.of("config.properties");
        if (!Files.exists(localConfig)) {
            throw new IllegalStateException(
                    "Missing config.properties in working directory: " + Path.of("").toAbsolutePath()
            );
        }

        Properties properties = new Properties();
        try (InputStream local = Files.newInputStream(localConfig)) {
            properties.load(local);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load config.properties", e);
        }

        Properties accountingInfo = loadAccountingInfo(properties);
        return new Configuration(properties, accountingInfo);
    }

    private static Properties loadAccountingInfo(Properties mainProperties) {
        String accountingInfoFile = mainProperties.getProperty("accounting.info.file", "config.accountinginfo").trim();
        Path path = Path.of(accountingInfoFile);

        if (!Files.exists(path)) {
            throw new IllegalStateException("Missing accounting info file: " + path.toAbsolutePath());
        }

        Properties accountingInfo = new Properties();
        try (InputStream input = Files.newInputStream(path)) {
            accountingInfo.load(input);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load accounting info file: " + path, e);
        }

        return accountingInfo;
    }

    public int getAccountingYear() {
        return Integer.parseInt(requireAccounting("accounting.year"));
    }

    public Set<String> getMtbProdStadiumAllowed() {
        return MtbLicenseFilter.parseAllowedList(requireAccounting("mtb.prod.stadium.allowed"));
    }

    public Set<String> getMtbFormalAllowed() {
        return MtbLicenseFilter.parseAllowedList(requireAccounting("mtb.formal.allowed"));
    }

    public String getMtbStagesFile() {
        return requireAccounting("mtb.stages.file");
    }

    public String getOutputFile() {
        return require("output.file");
    }

    public String getProffAquaInputFile() {
        return require("proffaqua.input.file");
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

    private String requireAccounting(String key) {
        String value = accountingInfo.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing required accounting info property: " + key);
        }
        return value.trim();
    }
}
