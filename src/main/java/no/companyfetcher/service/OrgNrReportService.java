package no.companyfetcher.service;

import no.companyfetcher.config.Configuration;
import no.companyfetcher.config.MtbStage;
import no.companyfetcher.config.MtbStageFeeCalculator;
import no.companyfetcher.config.MtbStagesLoader;
import no.companyfetcher.model.AquacultureCapacityData;
import no.companyfetcher.model.CompanyData;
import no.companyfetcher.model.OrgNrReportData;
import no.companyfetcher.model.ReportRunMode;
import no.companyfetcher.provider.CompanyProvider;
import no.companyfetcher.provider.DummyProvider;
import no.companyfetcher.provider.FiskeridirApiProvider;
import no.companyfetcher.provider.ProffApiProvider;
import no.companyfetcher.provider.ProffWebProvider;
import no.companyfetcher.provider.ProviderType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class OrgNrReportService {

    private static final Logger log = LoggerFactory.getLogger(OrgNrReportService.class);

    private final CompanyService companyService;
    private final AquacultureService aquacultureService;
    private final List<MtbStage> mtbStages;
    private final long requestDelayMs;

    public OrgNrReportService(Configuration configuration) {
        this.companyService = new CompanyService(createProffProvider(configuration), configuration);
        this.aquacultureService = new AquacultureService(new FiskeridirApiProvider(configuration), configuration);
        this.mtbStages = MtbStagesLoader.load(Path.of(configuration.getMtbStagesFile()));
        this.requestDelayMs = configuration.getRequestDelayMs();
    }

    public List<OrgNrReportData> fetchAll(List<String> orgNumbers) {
        return fetchAll(orgNumbers, ReportRunMode.PROFF_AND_MTB);
    }

    public List<OrgNrReportData> fetchAll(List<String> orgNumbers, ReportRunMode mode) {
        List<OrgNrReportData> results = new ArrayList<>(orgNumbers.size());
        int total = orgNumbers.size();

        for (int index = 0; index < orgNumbers.size(); index++) {
            String orgNumber = orgNumbers.get(index);
            int lineNo = index + 1;
            log.info("[{}/{}] {} - henter Proff-data", lineNo, total, orgNumber);

            CompanyData proff = companyService.fetchOne(orgNumber);
            logProffResult(lineNo, total, orgNumber, proff);

            AquacultureCapacityData aqua;
            if (mode == ReportRunMode.PROFF_ONLY) {
                log.info("[{}/{}] {} - hopper over akvakultur (Bare Proff)", lineNo, total, orgNumber);
                aqua = emptyAqua(orgNumber);
            } else {
                log.info("[{}/{}] {} - henter akvakulturdata", lineNo, total, orgNumber);
                aqua = aquacultureService.fetchOne(orgNumber);
                logAquaResult(lineNo, total, orgNumber, aqua);
            }

            OrgNrReportData report = combine(orgNumber, proff, aqua, mode);
            results.add(report);
            if (mode == ReportRunMode.PROFF_AND_MTB) {
                log.info("[{}/{}] {} - ferdig (Fee: {})", lineNo, total, orgNumber, report.fee());
            } else {
                log.info("[{}/{}] {} - ferdig", lineNo, total, orgNumber);
            }

            if (index < orgNumbers.size() - 1 && requestDelayMs > 0) {
                sleep(requestDelayMs);
            }
        }

        return results;
    }

    private AquacultureCapacityData emptyAqua(String orgNumber) {
        return new AquacultureCapacityData(orgNumber, null, null, null, 0, null);
    }

    private void logProffResult(int lineNo, int total, String orgNumber, CompanyData proff) {
        if (proff.status() != null && proff.status().startsWith("FAILED")) {
            log.warn("[{}/{}] {} - Proff feilet: {}", lineNo, total, orgNumber, proff.status());
            return;
        }

        String name = proff.companyName() == null ? orgNumber : proff.companyName();
        log.info("[{}/{}] {} - Proff OK: {}", lineNo, total, orgNumber, name);
    }

    private void logAquaResult(int lineNo, int total, String orgNumber, AquacultureCapacityData aqua) {
        if (aqua.status() != null && aqua.status().startsWith("FAILED")) {
            log.warn("[{}/{}] {} - Aqua feilet: {}", lineNo, total, orgNumber, aqua.status());
            return;
        }

        if ("OK: NO_MATCHING_MTB".equals(aqua.status())) {
            log.info("[{}/{}] {} - Aqua: ingen matfisk-tillatelser", lineNo, total, orgNumber);
            return;
        }

        log.info(
                "[{}/{}] {} - Aqua OK: {} {} ({} tillatelser)",
                lineNo,
                total,
                orgNumber,
                aqua.totalCapacity(),
                aqua.unit(),
                aqua.entryCount()
        );
    }

    private OrgNrReportData combine(
            String orgNumber,
            CompanyData proff,
            AquacultureCapacityData aqua,
            ReportRunMode mode
    ) {
        String orgName = proff.companyName() != null && !proff.companyName().isBlank()
                ? proff.companyName()
                : aqua.companyName();

        if (mode == ReportRunMode.PROFF_ONLY) {
            return new OrgNrReportData(
                    orgNumber,
                    orgName,
                    proff.accountingYear(),
                    proff.revenue(),
                    proff.salaryCost(),
                    proff.ebit(),
                    proff.status(),
                    null,
                    null,
                    0,
                    null,
                    null
            );
        }

        double capacity = aqua.totalCapacity() == null ? 0.0 : aqua.totalCapacity();
        long fee = MtbStageFeeCalculator.calculate(capacity, mtbStages);

        return new OrgNrReportData(
                orgNumber,
                orgName,
                proff.accountingYear(),
                proff.revenue(),
                proff.salaryCost(),
                proff.ebit(),
                proff.status(),
                aqua.totalCapacity(),
                aqua.unit(),
                aqua.entryCount(),
                aqua.status(),
                fee
        );
    }

    private static CompanyProvider createProffProvider(Configuration configuration) {
        ProviderType providerType = configuration.getProviderType();
        return switch (providerType) {
            case PROFF_WEB -> new ProffWebProvider(configuration);
            case PROFF_API -> new ProffApiProvider(configuration);
            case DUMMY -> new DummyProvider(configuration.getAccountingYear());
        };
    }

    private void sleep(long delayMs) {
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
