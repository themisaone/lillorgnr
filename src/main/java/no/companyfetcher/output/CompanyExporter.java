package no.companyfetcher.output;

import no.companyfetcher.model.CompanyData;

import java.util.List;

public interface CompanyExporter {

    void export(List<CompanyData> companies);
}
