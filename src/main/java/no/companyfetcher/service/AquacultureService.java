package no.companyfetcher.service;

import no.companyfetcher.config.Configuration;
import no.companyfetcher.model.AquacultureCapacityData;
import no.companyfetcher.provider.AquacultureProvider;
import no.companyfetcher.provider.ProviderException;
import no.companyfetcher.util.RetryExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class AquacultureService {

    private static final Logger log = LoggerFactory.getLogger(AquacultureService.class);

    private final AquacultureProvider provider;
    private final RetryExecutor retryExecutor;
    private final long requestDelayMs;

    public AquacultureService(AquacultureProvider provider, Configuration configuration) {
        this.provider = provider;
        this.retryExecutor = new RetryExecutor(configuration.getRetryCount());
        this.requestDelayMs = configuration.getRequestDelayMs();
    }

    public List<AquacultureCapacityData> fetchAll(List<String> orgNumbers) {
        List<AquacultureCapacityData> results = new ArrayList<>(orgNumbers.size());

        for (int index = 0; index < orgNumbers.size(); index++) {
            String orgNumber = orgNumbers.get(index);
            log.info("Fetching aquaculture capacity for {}", orgNumber);

            AquacultureCapacityData data = fetchOne(orgNumber);
            results.add(data);

            if (data.status() != null && data.status().startsWith("FAILED")) {
                log.warn("FAILED {} ({})", orgNumber, data.status());
            } else {
                log.info(
                        "SUCCESS {} totalCapacity={} {} ({} entries)",
                        data.companyName() == null ? orgNumber : data.companyName(),
                        data.totalCapacity(),
                        data.unit(),
                        data.entryCount()
                );
            }

            if (index < orgNumbers.size() - 1 && requestDelayMs > 0) {
                sleep(requestDelayMs);
            }
        }

        return results;
    }

    public AquacultureCapacityData fetchOne(String orgNumber) {
        return fetchSingle(orgNumber);
    }

    private AquacultureCapacityData fetchSingle(String orgNumber) {
        try {
            return retryExecutor.execute("Fetch aquaculture " + orgNumber, () -> provider.fetch(orgNumber));
        } catch (ProviderException e) {
            return AquacultureCapacityData.failed(orgNumber, e.getReason().name());
        } catch (RuntimeException e) {
            log.error("Unexpected error fetching aquaculture data for {}", orgNumber, e);
            return AquacultureCapacityData.failed(orgNumber, "UNEXPECTED");
        }
    }

    private void sleep(long delayMs) {
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
