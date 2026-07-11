package no.companyfetcher.service;

import no.companyfetcher.config.Configuration;

final class TestAccountingConfiguration {

    private TestAccountingConfiguration() {
    }

    static Configuration load() {
        return Configuration.load();
    }
}
