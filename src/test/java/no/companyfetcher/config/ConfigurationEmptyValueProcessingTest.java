package no.companyfetcher.config;

import no.companyfetcher.output.EmptyValueProcessing;
import org.junit.jupiter.api.Test;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ConfigurationEmptyValueProcessingTest {

    @Test
    void defaultsToUntouchedWhenPropertyIsMissing() {
        Configuration configuration = new Configuration(new Properties(), new Properties());

        assertEquals(EmptyValueProcessing.UNTOUCHED, configuration.getEmptyValueProcessing());
    }

    @Test
    void readsClearFromConfigProperties() {
        Properties properties = new Properties();
        properties.setProperty("empty.value.processing", "CLEAR");

        Configuration configuration = new Configuration(properties, new Properties());

        assertEquals(EmptyValueProcessing.CLEAR, configuration.getEmptyValueProcessing());
    }

    @Test
    void readsUntouchedFromConfigProperties() {
        Properties properties = new Properties();
        properties.setProperty("empty.value.processing", "untouched");

        Configuration configuration = new Configuration(properties, new Properties());

        assertEquals(EmptyValueProcessing.UNTOUCHED, configuration.getEmptyValueProcessing());
    }

    @Test
    void rejectsInvalidConfigValues() {
        Properties properties = new Properties();
        properties.setProperty("empty.value.processing", "DELETE");

        Configuration configuration = new Configuration(properties, new Properties());

        assertThrows(IllegalStateException.class, configuration::getEmptyValueProcessing);
    }
}
