package no.companyfetcher.service;

import no.companyfetcher.config.Configuration;
import no.companyfetcher.model.CompanyData;
import no.companyfetcher.provider.CompanyProvider;
import no.companyfetcher.provider.ProviderException;
import no.companyfetcher.util.RetryExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class CompanyService {

    private static final Logger log = LoggerFactory.getLogger(CompanyService.class);

    private final CompanyProvider provider;
    private final RetryExecutor retryExecutor;
    private final long requestDelayMs;

    public CompanyService(CompanyProvider provider, Configuration configuration) {
        this.provider = provider;
        this.retryExecutor = new RetryExecutor(configuration.getRetryCount());
        this.requestDelayMs = configuration.getRequestDelayMs();
    }

    public List<CompanyData> fetchAll(List<String> orgNumbers) {
        List<CompanyData> results = new ArrayList<>(orgNumbers.size());

        for (int index = 0; index < orgNumbers.size(); index++) {
            String orgNumber = orgNumbers.get(index);
            log.info("Fetching {}", orgNumber);

            CompanyData company = fetchOne(orgNumber);
            results.add(company);

            if (company.status() != null && company.status().startsWith("FAILED")) {
                log.warn("FAILED {} ({})", orgNumber, company.status());
            } else {
                log.info("SUCCESS {}", company.companyName() == null ? orgNumber : company.companyName());
            }

            if (index < orgNumbers.size() - 1 && requestDelayMs > 0) {
                sleep(requestDelayMs);
            }
        }

        return results;
    }

    public CompanyData fetchOne(String orgNumber) {
        return fetchSingle(orgNumber);
    }

    private CompanyData fetchSingle(String orgNumber) {
        try {
            return retryExecutor.execute("Fetch " + orgNumber, () -> provider.fetch(orgNumber));
        } catch (ProviderException e) {
            return CompanyData.failed(orgNumber, e.getReason().name());
        } catch (RuntimeException e) {
            log.error("Unexpected error fetching {}", orgNumber, e);
            return CompanyData.failed(orgNumber, "UNEXPECTED");
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
