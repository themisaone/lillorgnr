package no.companyfetcher.provider;

import no.companyfetcher.model.AquacultureCapacityData;

public interface AquacultureProvider {

    AquacultureCapacityData fetch(String orgNumber);
}
