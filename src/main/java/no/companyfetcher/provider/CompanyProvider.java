package no.companyfetcher.provider;

import no.companyfetcher.model.CompanyData;

public interface CompanyProvider {

    CompanyData fetch(String orgNumber);
}
